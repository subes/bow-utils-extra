package be.bagofwords.application.status.perf;

import be.bagofwords.application.status.StatusViewable;

import java.util.*;

public class ThreadStatusViewable implements StatusViewable {

    private static final String indentation = "&nbsp;";

    @Override
    public void printHtmlStatus(StringBuilder sb) {
        Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
        List<Thread> sortedThreads = new ArrayList<>(stackTraces.keySet());
        Collections.sort(sortedThreads, new Comparator<Thread>() {
            @Override
            public int compare(Thread o1, Thread o2) {
                if (o1.getState() != o2.getState()) {
                    if (o1.getState() == Thread.State.RUNNABLE) {
                        return -1;
                    } else if (o2.getState() == Thread.State.RUNNABLE) {
                        return 1;
                    } else {
                        return o1.getState().ordinal() - o2.getState().ordinal();
                    }
                } else {
                    return o1.getName().compareTo(o2.getName());
                }
            }
        });
        sb.append("<h1>Threads</h1>");
        int numOfJettyThreads = 0;
        for (Thread thread : sortedThreads) {
            if (!thread.getName().contains("qtp")) {
                printStackTrace(sb, thread, stackTraces.get(thread));
            } else {
                numOfJettyThreads++;
            }
        }
        if (numOfJettyThreads > 0) {
            sb.append("<br>Hidden " + numOfJettyThreads + " jetty threads");
        }
    }

    private void printStackTrace(StringBuilder sb, Thread thread, StackTraceElement[] stackTraceElements) {
        sb.append("<b>Thread " + thread.getName() + " " + thread.getState() + "</b><br>");
        String combinedIndentation = indentation;
        for (StackTraceElement element : stackTraceElements) {
            sb.append(combinedIndentation + element.getClassName() + "." + element.getMethodName() + "(" + element.getFileName() + ":" + element.getLineNumber() + ")<br>");
            combinedIndentation += indentation;
        }

    }
}
