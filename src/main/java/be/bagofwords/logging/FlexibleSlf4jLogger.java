package be.bagofwords.logging;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

public class FlexibleSlf4jLogger extends MarkerIgnoringBase {

    private final String name;

    public FlexibleSlf4jLogger(String name) {
        this.name = name;
    }

    public static LogLevel toLevel(int level) {
        if (level >= 40) {
            return LogLevel.ERROR;
        } else if (level >= 30) {
            return LogLevel.WARN;
        } else {
            return LogLevel.INFO;
        }
    }

    private void log(int level, String message, Throwable t) {
        if (this.isLevelEnabled(level)) {
            FlexibleSl4jLogFactory.INSTANCE.getLogImpl().log(level, name, message, t);
        }
    }

    private void log(int level, String format, Object arg1, Object arg2) {
        if (this.isLevelEnabled(level)) {
            FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
            this.log(level, tp.getMessage(), tp.getThrowable());
        }
    }

    private void log(int level, String format, Object... arguments) {
        if (this.isLevelEnabled(level)) {
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            this.log(level, tp.getMessage(), tp.getThrowable());
        }
    }

    protected boolean isLevelEnabled(int logLevel) {
        return logLevel >= FlexibleSl4jLogFactory.LOG_LEVEL;
    }

    public boolean isTraceEnabled() {
        return this.isLevelEnabled(0);
    }

    public void trace(String msg) {
        this.log(0, msg, (Throwable) null);
    }

    public void trace(String format, Object param1) {
        this.log(0, format, param1, null);
    }

    public void trace(String format, Object param1, Object param2) {
        this.log(0, format, param1, param2);
    }

    public void trace(String format, Object... argArray) {
        this.log(0, format, argArray);
    }

    public void trace(String msg, Throwable t) {
        this.log(0, msg, t);
    }

    public boolean isDebugEnabled() {
        return this.isLevelEnabled(10);
    }

    public void debug(String msg) {
        this.log(10, msg, (Throwable) null);
    }

    public void debug(String format, Object param1) {
        this.log(10, format, param1, null);
    }

    public void debug(String format, Object param1, Object param2) {
        this.log(10, format, param1, param2);
    }

    public void debug(String format, Object... argArray) {
        this.log(10, format, argArray);
    }

    public void debug(String msg, Throwable t) {
        this.log(10, msg, t);
    }

    public boolean isInfoEnabled() {
        return this.isLevelEnabled(20);
    }

    public void info(String msg) {
        this.log(20, msg, (Throwable) null);
    }

    public void info(String format, Object arg) {
        this.log(20, format, arg, null);
    }

    public void info(String format, Object arg1, Object arg2) {
        this.log(20, format, arg1, arg2);
    }

    public void info(String format, Object... argArray) {
        this.log(20, format, argArray);
    }

    public void info(String msg, Throwable t) {
        this.log(20, msg, t);
    }

    public boolean isWarnEnabled() {
        return this.isLevelEnabled(30);
    }

    public void warn(String msg) {
        this.log(30, msg, (Throwable) null);
    }

    public void warn(String format, Object arg) {
        this.log(30, format, arg, null);
    }

    public void warn(String format, Object arg1, Object arg2) {
        this.log(30, format, arg1, arg2);
    }

    public void warn(String format, Object... argArray) {
        this.log(30, format, argArray);
    }

    public void warn(String msg, Throwable t) {
        this.log(30, msg, t);
    }

    public boolean isErrorEnabled() {
        return this.isLevelEnabled(40);
    }

    public void error(String msg) {
        this.log(40, msg, (Throwable) null);
    }

    public void error(String format, Object arg) {
        this.log(40, format, arg, null);
    }

    public void error(String format, Object arg1, Object arg2) {
        this.log(40, format, arg1, arg2);
    }

    public void error(String format, Object... argArray) {
        this.log(40, format, argArray);
    }

    public void error(String msg, Throwable t) {
        this.log(40, msg, t);
    }

}

