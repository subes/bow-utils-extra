package be.bagofwords.logging;

public class SplitterLogImpl implements LogImpl {

    private final LogImpl[] logImplementations;

    public SplitterLogImpl(LogImpl... logImplementations) {
        this.logImplementations = logImplementations;
    }

    @Override
    public void log(int level, String logger, String message, Throwable throwable) {
        for (LogImpl logImplementation : logImplementations) {
            logImplementation.log(level, logger, message, throwable);
        }
    }
}
