/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-7-29. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package org.slf4j.impl;

import be.bagofwords.logging.FlexibleSl4jLogFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

public class StaticLoggerBinder implements LoggerFactoryBinder {

    private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

    private static FlexibleSl4jLogFactory flexibleSl4jLogFactory = FlexibleSl4jLogFactory.INSTANCE;

    public static final StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    // to avoid constant folding by the compiler, this field must *not* be final
    public static String REQUESTED_API_VERSION = "1.6.99"; // !final

    private static final String loggerFactoryClassStr = FlexibleSl4jLogFactory.class.getName();

    public ILoggerFactory getLoggerFactory() {
        return flexibleSl4jLogFactory;
    }

    public String getLoggerFactoryClassStr() {
        return loggerFactoryClassStr;
    }
}
