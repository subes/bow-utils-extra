package be.bagofwords.application.status.perf;

import be.bagofwords.counts.Counter;
import be.bagofwords.util.NumUtils;

import java.util.ArrayList;
import java.util.List;

public class ThreadSamplesPrinter {

    private static final double MIN_FRACTION = 0.01;

    public static void printTopTraces(StringBuilder result, Counter<Trace> traces, int numOfSamples) {
        List<Trace> sortedTraces = new ArrayList<>(traces.sortedKeys());
        for (int i = 0; i < sortedTraces.size(); i++) {
            Trace parentTrace = sortedTraces.get(i);
            if (parentTrace.getParent() == null) {
                printTrace(0, "", result, parentTrace, traces, sortedTraces, false, numOfSamples);
                result.append("\n");
            }
        }
    }

    private static void printTrace(int level, String combinedIndentation, StringBuilder result, Trace parentTrace, Counter<Trace> traces, List<Trace> sortedTraces, boolean printHorizontalLine, int numOfSamples) {
        double fraction = traces.get(parentTrace) / (double) numOfSamples;
        if (fraction > MIN_FRACTION) {
            String indentation = combinedIndentation + (printHorizontalLine ? "\\" : " ");
            int numOfChildren = countNumberOfChildren(parentTrace, traces, sortedTraces, numOfSamples);
            String line = parentTrace.getLine();
            if (level == 0) {
                line = "THREAD " + line.toUpperCase();
            }
            result.append(indentation + NumUtils.makeNicePercent(fraction) + "% " + line + "\n");
            //Add subtraces
            int numOfChildrenPrinted = 0;
            for (Trace subtrace : sortedTraces) {
                if (subtrace.getParent() != null && subtrace.getParent().equals(parentTrace)) {
                    char trackingLine = level % 2 == 0 ? '|' : '!';
                    printTrace(level + 1, combinedIndentation + " " + (numOfChildrenPrinted < numOfChildren - 1 ? " " + trackingLine : "  "), result, subtrace, traces, sortedTraces, numOfChildren > 0, numOfSamples);
                    numOfChildrenPrinted++;
                }
            }
        }
    }

    private static int countNumberOfChildren(Trace parentTrace, Counter<Trace> traces, List<Trace> sortedTraces, int numOfSamples) {
        int numOfChildren = 0;
        for (Trace subtrace : sortedTraces) {
            if (subtrace.getParent() != null && subtrace.getParent().equals(parentTrace)) {
                double fraction = traces.get(subtrace) / (double) numOfSamples;
                if (fraction > MIN_FRACTION) {
                    numOfChildren++;
                }
            }
        }
        return numOfChildren;
    }
}
