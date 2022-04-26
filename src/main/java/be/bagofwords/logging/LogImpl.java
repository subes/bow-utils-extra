package be.bagofwords.logging;

/**
 * Created by koen on 27/04/17.
 */
public interface LogImpl {

    void log(int level, String logger, String message, Throwable throwable);

}
