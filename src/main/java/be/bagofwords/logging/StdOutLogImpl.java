package be.bagofwords.logging;

import be.bagofwords.util.Utils;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by koen on 27/04/17.
 */
public class StdOutLogImpl implements LogImpl {

    private final static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void log(int numericalLevel, String loggingClass, String message, Throwable throwable) {
        LogLevel level = FlexibleSlf4jLogger.toLevel(numericalLevel);
        PrintStream stream = level == LogLevel.INFO ? System.out : System.err;
        String time = TIME_FORMAT.format(new Date());
        if (throwable != null) {
            stream.println(time + " " + level + " " + loggingClass + "\t" + message + " " + throwable.getMessage());
            stream.println(Utils.getStackTrace(throwable));
        } else {
            stream.println(time + " " + level + " " + loggingClass + "\t" + message);
        }
    }
}
