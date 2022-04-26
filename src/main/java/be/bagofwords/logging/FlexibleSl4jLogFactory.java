package be.bagofwords.logging;

import be.bagofwords.util.StringUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class FlexibleSl4jLogFactory implements ILoggerFactory {

    public final static FlexibleSl4jLogFactory INSTANCE = new FlexibleSl4jLogFactory();
    public static int LOG_LEVEL;

    private final Map<String, Logger> loggerMap = new HashMap<>();
    private final WeakHashMap<ThreadGroup, LogImpl> logImplementationsMap = new WeakHashMap<>();
    private LogImpl defaultLogImplementation = initializeDefaultImplementation();

    private LogImpl initializeDefaultImplementation() {
        String implementation = System.getProperty("flexible-slf4j.default.implementation");
        String levelString = System.getProperty("flexible-slf4j.log.level");
        if (levelString == null) {
            levelString = "INFO";
        }
        LOG_LEVEL = stringToLevel(levelString);

        if (StringUtils.isNotEmpty(implementation)) {
            try {
                return (LogImpl) getClass().getClassLoader().loadClass(implementation).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new RuntimeException("Failed to load default implementation " + implementation, e);
            }
        } else {
            return new StdOutLogImpl();
        }
    }

    private static int stringToLevel(String levelStr) {
        return "trace".equalsIgnoreCase(levelStr) ? 0 : ("debug".equalsIgnoreCase(levelStr) ? 10 : ("info".equalsIgnoreCase(levelStr) ? 20 : ("warn".equalsIgnoreCase(levelStr) ? 30 : ("error".equalsIgnoreCase(levelStr) ? 40 : ("off".equalsIgnoreCase(levelStr) ? 50 : 20)))));
    }

    @Override
    public synchronized Logger getLogger(String name) {
        if (!this.loggerMap.containsKey(name)) {
            this.loggerMap.put(name, new FlexibleSlf4jLogger(name));
        }
        return this.loggerMap.get(name);
    }

    public LogImpl getLogImpl() {
        LogImpl impl = logImplementationsMap.get(Thread.currentThread().getThreadGroup());
        if (impl == null) {
            return defaultLogImplementation;
        } else {
            return impl;
        }
    }

    public void setLoggerImplementation(Thread thread, LogImpl log) {
        logImplementationsMap.put(thread.getThreadGroup(), log);
    }

    public void setDefaultLogImplementation(LogImpl logImplementation) {
        this.defaultLogImplementation = logImplementation;
    }
}
