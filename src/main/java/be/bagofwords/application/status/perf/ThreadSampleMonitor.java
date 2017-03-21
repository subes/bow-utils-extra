package be.bagofwords.application.status.perf;

import be.bagofwords.counts.Counter;
import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.LifeCycleBean;
import be.bagofwords.ui.UI;
import be.bagofwords.util.SafeThread;
import be.bagofwords.util.Utils;
import be.bagofwords.web.BaseController;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import spark.Request;
import spark.Response;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class ThreadSampleMonitor extends BaseController implements LifeCycleBean {

    public static final int MAX_NUM_OF_SAMPLES = 10000;

    private final TraceSampler traceSampler;
    private final Counter<Trace> relevantTracesCounter;
    private final Counter<Trace> lessRelevantTracesCounter;
    private int numOfSamples;

    private boolean saveThreadSamplesToFile;
    private String locationForSavedThreadSamples;
    private String applicationName;


    public ThreadSampleMonitor(ApplicationContext applicationContext) {
        super("/perf");
        this.saveThreadSamplesToFile = Boolean.parseBoolean(applicationContext.getProperty("save_thread_samples_to_file", "false"));
        if (this.saveThreadSamplesToFile) {
            this.locationForSavedThreadSamples = applicationContext.getProperty("location_for_saved_thread_samples");
        }
        this.applicationName = applicationContext.getApplicationName();
        this.relevantTracesCounter = new Counter<>();
        this.lessRelevantTracesCounter = new Counter<>();
        this.traceSampler = new TraceSampler();
        this.numOfSamples = 0;
    }

    @Override
    protected synchronized String handleRequest(Request request, Response response) throws Exception {
        StringBuilder result = new StringBuilder();
        synchronized (relevantTracesCounter) {
            synchronized (lessRelevantTracesCounter) {
                result.append("Collected " + relevantTracesCounter.getTotal() + " samples.");
                result.append("<h1>Relevant traces</h1><pre>");
                ThreadSamplesPrinter.printTopTraces(result, relevantTracesCounter, numOfSamples);
                result.append("</pre>");
                result.append("<h1>Other traces</h1><pre>");
                ThreadSamplesPrinter.printTopTraces(result, lessRelevantTracesCounter, numOfSamples);
                result.append("</pre>");
            }
        }
        return result.toString();
    }

    @Override
    public void startBean() {
        this.traceSampler.start();
    }

    @Override
    public void stopBean() {
        traceSampler.terminateAndWaitForFinish();
        if (saveThreadSamplesToFile) {
            saveThreadSamplesToFile();
        }
    }

    private void saveThreadSamplesToFile() {
        try {
            synchronized (relevantTracesCounter) {
                synchronized (lessRelevantTracesCounter) {
                    File file = new File(locationForSavedThreadSamples + "_" + applicationName + ".txt");
                    StringBuilder sb = new StringBuilder();
                    sb.append("Traces for " + applicationName + " on " + DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm") + "\n\n");
                    sb.append("-- Relevant traces --\n\n");
                    ThreadSamplesPrinter.printTopTraces(sb, relevantTracesCounter, numOfSamples);
                    sb.append("\n\n-- Less relevant traces --\n\n");
                    ThreadSamplesPrinter.printTopTraces(sb, lessRelevantTracesCounter, numOfSamples);
                    FileUtils.writeStringToFile(file, sb.toString(), "UTF-8");
                }
            }
        } catch (IOException exp) {
            UI.writeError("Failed to save thread samples!", exp);
        }
    }

    public void clearSamples() {
        synchronized (relevantTracesCounter) {
            relevantTracesCounter.clear();
        }
        synchronized (lessRelevantTracesCounter) {
            lessRelevantTracesCounter.clear();
        }
        numOfSamples = 0;
    }

    public Counter<Trace> getRelevantTracesCounter() {
        return relevantTracesCounter;
    }

    public Counter<Trace> getLessRelevantTracesCounter() {
        return lessRelevantTracesCounter;
    }

    private class TraceSampler extends SafeThread {

        public TraceSampler() {
            super("traceSampler", true);
        }

        @Override
        protected void runInt() throws Exception {
            while (!isTerminateRequested()) {
                Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
                numOfSamples++;
                for (Thread thread : stackTraces.keySet()) {
                    StackTraceElement[] thisTrace = stackTraces.get(thread);
                    String threadName = thread.getName();
                    if (!threadName.equals("traceSampler") && thisTrace.length > 0) {
                        String methodName = thisTrace[0].getMethodName();
                        boolean notRelevantThread = threadName.equals("SparkServerThread") || threadName.equals("Signal Dispatcher") || threadName.equals("Finalizer");
                        notRelevantThread |= threadName.equals("DateCache") || threadName.startsWith("qtp") || threadName.equals("Reference Handler") || threadName.startsWith("HashSessionScavenger");
                        notRelevantThread |= methodName.equals("accept0") || methodName.equals("accept") || methodName.equals("epollWait") || methodName.equals("socketAccept");
                        notRelevantThread |= threadName.equals("ChangedValueListener") && methodName.equals("socketRead0");
                        notRelevantThread |= methodName.equals("park") && thisTrace[0].getClassName().equals("Unsafe");
                        notRelevantThread |= inReadNextActionMethod(threadName, thisTrace);
                        String normalizedThreadName = threadName.replaceAll("[^A-Za-z]", "");
                        Trace parent = new Trace(normalizedThreadName, null);
                        addTrace(notRelevantThread, parent);
                        for (int i = thisTrace.length - 1; i >= 0; i--) {
                            StackTraceElement element = thisTrace[i];
                            Trace trace = new Trace(element.getClassName() + "." + element.getMethodName() + "(" + element.getFileName() + ":" + element.getLineNumber() + ")", parent);
                            addTrace(notRelevantThread, trace);
                            parent = trace;
                        }
                    }
                }
                synchronized (relevantTracesCounter) {
                    relevantTracesCounter.trim(MAX_NUM_OF_SAMPLES / 2);
                }
                synchronized (lessRelevantTracesCounter) {
                    lessRelevantTracesCounter.trim(MAX_NUM_OF_SAMPLES / 2);
                }
                Utils.threadSleep(200);
            }
        }

        private void addTrace(boolean notRelevantThread, Trace trace) {
            if (notRelevantThread) {
                synchronized (lessRelevantTracesCounter) {
                    lessRelevantTracesCounter.inc(trace);
                }
            } else {
                synchronized (relevantTracesCounter) {
                    relevantTracesCounter.inc(trace);
                }
            }
        }

        private boolean inReadNextActionMethod(String threadName, StackTraceElement[] thisTrace) {
            if (threadName.startsWith("DatabaseServerRequestHandler")) {
                for (StackTraceElement traceElement : thisTrace) {
                    if (traceElement.getMethodName().equals("readNextAction")) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    public int getNumOfSamples() {
        return numOfSamples;
    }
}
