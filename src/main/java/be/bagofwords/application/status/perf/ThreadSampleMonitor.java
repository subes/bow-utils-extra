package be.bagofwords.application.status.perf;

import be.bagofwords.counts.Counter;
import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.LifeCycleBean;
import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.web.BaseController;
import spark.Request;
import spark.Response;

public class ThreadSampleMonitor extends BaseController implements LifeCycleBean {

    private boolean saveThreadSamplesToFile;
    private String locationForSavedThreadSamples;

    @Inject
    private CollectSamplesService collectSamplesService;

    public ThreadSampleMonitor(ApplicationContext applicationContext) {
        super("/perf");
        this.saveThreadSamplesToFile = Boolean.parseBoolean(applicationContext.getProperty("save.thread.samples.to.file", "bow-utils-extra.properties"));
        if (this.saveThreadSamplesToFile) {
            this.locationForSavedThreadSamples = applicationContext.getProperty("location.for.saved.thread.samples");
        }
    }

    @Override
    protected synchronized String handleRequest(Request request, Response response) throws Exception {
        StringBuilder result = new StringBuilder();
        Counter<Trace> relevantTracesCounter = collectSamplesService.getRelevantTracesCounter();
        Counter<Trace> lessRelevantTracesCounter = collectSamplesService.getLessRelevantTracesCounter();
        int numOfSamples = collectSamplesService.getNumOfSamples();
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
    }

    @Override
    public void stopBean() {
        if (saveThreadSamplesToFile) {
            collectSamplesService.saveThreadSamplesToFile(locationForSavedThreadSamples);
        }
    }


}
