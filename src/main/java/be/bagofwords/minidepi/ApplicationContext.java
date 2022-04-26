/*******************************************************************************
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-11. For license
 * information see the LICENSE file in the root folder of this repository.
 ******************************************************************************/

package be.bagofwords.minidepi;

import be.bagofwords.minidepi.implementation.BeanManager;
import be.bagofwords.minidepi.implementation.LifeCycleManager;
import be.bagofwords.minidepi.implementation.PropertyManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static be.bagofwords.minidepi.properties.PropertyUtils.mapToProperties;

public class ApplicationContext implements AutoCloseable {

    private final PropertyManager propertyManager;
    private final LifeCycleManager lifeCycleManager;
    private final BeanManager beanManager;

    public ApplicationContext() {
        this((ApplicationContext) null);
    }

    public ApplicationContext(Map<String, String> config) {
        this(null, mapToProperties(config));
    }

    public ApplicationContext(Properties properties) {
        this(null, properties);
    }

    public ApplicationContext(ApplicationContext parentContext) {
        this(Collections.<String, String>emptyMap(), parentContext);
    }

    public ApplicationContext(Map<String, String> config, ApplicationContext parentContext) {
        this(parentContext, mapToProperties(config));
    }

    public ApplicationContext(ApplicationContext parentContext, Properties properties) {
        propertyManager = new PropertyManager(properties);
        lifeCycleManager = new LifeCycleManager(this);
        beanManager = new BeanManager(this, lifeCycleManager, parentContext != null ? parentContext.beanManager : null);
        if (Boolean.parseBoolean(getProperty("autostart.application", "mini-dep-i.properties"))) {
            lifeCycleManager.startApplication();
        }
    }

    public void start() {
        lifeCycleManager.startApplication();
    }

    public void terminate() {
        lifeCycleManager.terminateApplication();
    }

    public void restart() {
        lifeCycleManager.restartApplication();
    }

    public void terminateAsync() {
        new Thread("terminate-application-thread") {
            public void run() {
                terminate();
            }
        }.start();
    }

    public boolean isStarted() {
        return lifeCycleManager.applicationIsStarted();
    }

    public boolean hasWiredFields(Object object) {
        return beanManager.hasWiredFields(object);
    }

    public boolean terminateWasRequested() {
        return lifeCycleManager.terminateWasRequested();
    }

    public synchronized void registerRuntimeDependency(Object bean, Object dependency) {
        lifeCycleManager.registerStartBeforeDependency(bean, dependency);
    }

    public void waitUntilTerminated() {
        lifeCycleManager.waitUntilTerminated();
    }

    public void registerBean(Object bean, String... names) {
        beanManager.registerBean(bean, names);
    }

    public void registerBean(Class beanClass, String... names) {
        beanManager.registerBean(beanClass, names);
    }

    public void wireBean(Object bean) {
        beanManager.wireBean(bean);
    }

    public <T> List<T> getBeans(Class<T> type, String... names) {
        return beanManager.getBeans(type, names);
    }

    public <T> T getBean(Class<T> beanType, String... names) {
        return beanManager.getBean(beanType, names);
    }

    public <T> T getBeanIfPresent(Class<T> beanType, String... names) {
        return beanManager.getBeanIfPresent(beanType, names);
    }

    public String getProperty(String name) {
        return propertyManager.getProperty(name);
    }

    public String getProperty(String name, String orFrom) {
        return propertyManager.getProperty(name, orFrom);
    }

    public void setProperty(String name, String value) {
        propertyManager.setProperty(name, value);
    }

    @Override
    public void close() {
        terminate();
    }
}
