/*******************************************************************************
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-11. For license
 * information see the LICENSE file in the root folder of this repository.
 ******************************************************************************/

package be.bagofwords.minidepi.implementation;

import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.ApplicationContextException;
import be.bagofwords.minidepi.LifeCycleBean;
import be.bagofwords.util.MappedLists;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static be.bagofwords.minidepi.implementation.ApplicationState.*;
import static java.util.Collections.singletonList;

public class LifeCycleManager {

    private final Set<Object> stoppedBeans = new HashSet<>();
    private final Set<LifeCycleBean> beansBeingStopped = new HashSet<>();
    private final Set<LifeCycleBean> beansBeingStarted = new HashSet<>();
    private final Set<LifeCycleBean> startedBeans = new HashSet<>();
    private final MappedLists<Object, Object> runtimeDependencies = new MappedLists<>();
    private final ApplicationContext applicationContext;
    private final Object terminateLock = new Object();

    private ApplicationState applicationState;

    public LifeCycleManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.resetState();
    }

    private void resetState() {
        applicationState = BEFORE_START;
        startedBeans.clear();
        stoppedBeans.clear();
    }

    public synchronized void startApplication() {
        if (applicationState != BEFORE_START) {
            throw new ApplicationContextException("Application can not be started because the current state is " + applicationState);
        }
        applicationState = STARTED;
        List<? extends LifeCycleBean> lifeCycleBeans = applicationContext.getBeans(LifeCycleBean.class);
        waitUntilBeansStarted(lifeCycleBeans);
    }

    public synchronized void terminateApplication() {
        if (!terminateWasRequested()) {
            applicationState = TERMINATE_REQUESTED;
            List<? extends LifeCycleBean> lifeCycleBeans = applicationContext.getBeans(LifeCycleBean.class);
            waitUntilBeansStopped(lifeCycleBeans);
            applicationState = TERMINATED;
            synchronized (terminateLock) {
                terminateLock.notifyAll();
            }
            Log.i("Application has terminated. Bye!");
        } else {
            Log.w("Application termination requested while application was already terminated");
        }
    }

    public void restartApplication() {
        if (applicationState == STARTED) {
            terminateApplication();
        }
        resetState();
        startApplication();
    }

    public synchronized void waitUntilBeanStopped(Object bean) {
        if (stoppedBeans.contains(bean)) {
            return;
        }
        if (bean instanceof LifeCycleBean) {
            if (runtimeDependencies.containsKey(bean)) {
                waitUntilBeansStopped(runtimeDependencies.get(bean));
            }
            if (beansBeingStopped.contains(bean)) {
                throw new ApplicationContextException("The stop() method of bean " + bean + " was already called. Possible cycle?");
            }
            beansBeingStopped.add((LifeCycleBean) bean);
            // Log.i("Stopping bean " + bean);
            ((LifeCycleBean) bean).stopBean();
            beansBeingStopped.remove(bean);
        }
        stoppedBeans.add(bean);
    }

    public synchronized void waitUntilBeansStopped(List<? extends Object> beans) {
        for (Object bean : beans) {
            waitUntilBeanStopped(bean);
        }
    }

    public synchronized void waitUntilBeanStarted(LifeCycleBean bean) {
        if (applicationState != STARTED) {
            throw new ApplicationContextException("The application was not yet started");
        }
        if (beansBeingStarted.contains(bean)) {
            throw new ApplicationContextException("The stop() method of bean " + bean + " was already called. Possible cycle?");
        }
        if (startedBeans.contains(bean)) {
            return;
        }
        beansBeingStarted.add(bean);
        // Log.i("Starting bean " + bean);
        bean.startBean();
        beansBeingStarted.remove(bean);
        startedBeans.add(bean);
    }

    public <T extends LifeCycleBean> void waitUntilBeansStarted(List<T> beans) {
        for (T bean : beans) {
            waitUntilBeanStarted(bean);
        }
    }

    public void waitUntilTerminated() {
        try {
            synchronized (terminateLock) {
                terminateLock.wait();
            }
        } catch (InterruptedException e) {
            if (applicationState != TERMINATE_REQUESTED) {
                throw new RuntimeException("Received InterruptedException while no termination was requested");
            }
        }
    }

    public boolean applicationIsStarted() {
        return applicationState == STARTED;
    }

    public void ensureBeanCorrectState(LifeCycleBean bean) {
        if (applicationState == STARTED) {
            waitUntilBeanStarted(bean);
        } else if (terminateWasRequested()) {
            waitUntilBeanStopped(bean);
        }
    }

    public boolean terminateWasRequested() {
        return applicationState == TERMINATE_REQUESTED || applicationState == TERMINATED;
    }

    public void registerStartBeforeDependency(Object bean, Object dependencyBean) {
        if (terminateWasRequested()) {
            throw new RuntimeException("Terminate was already requested. Please call registerStartBeforeDependency(..) on application startup");
        }
        if (isDependent(bean, dependencyBean)) {
            throw new RuntimeException("Cyclic dependency detected between beans " + bean + " and " + dependencyBean + ". Consider setting one of the inject annotations with ensureStarted=false");
        }
        runtimeDependencies.get(dependencyBean).add(bean);
    }

    private boolean isDependent(Object bean, Object dependencyBean) {
        List<List<Object>> dependencyChains = new ArrayList<>();
        dependencyChains.add(singletonList(bean));
        List<List<Object>> circularDependencyChains = new ArrayList<>();
        while (!dependencyChains.isEmpty()) {
            List<Object> dependencyChain = dependencyChains.remove(dependencyChains.size() - 1);
            Object lastBean = dependencyChain.get(dependencyChain.size() - 1);
            List<Object> dependencies = runtimeDependencies.get(lastBean);
            for (Object dependency : dependencies) {
                boolean isCircular = dependencyChain.contains(dependency) || dependency == dependencyBean;
                List<Object> newDependencyChain = new ArrayList<>(dependencyChain);
                newDependencyChain.add(dependency);
                if (isCircular) {
                    circularDependencyChains.add(newDependencyChain);
                } else {
                    dependencyChains.add(newDependencyChain);
                }
            }
        }
        return hasChainWithMultipleLifeCycleBeans(circularDependencyChains);
    }

    private boolean hasChainWithMultipleLifeCycleBeans(List<List<Object>> dependencyChains) {
        for (List<Object> dependencyChain : dependencyChains) {
            if (hasMultipleLifeCycleBeans(dependencyChain)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMultipleLifeCycleBeans(List<Object> circularDependencyChain) {
        int numOfLifecycleBeans = 0;
        for (Object o : circularDependencyChain) {
            if (o instanceof LifeCycleBean) {
                numOfLifecycleBeans++;
            }
        }
        return numOfLifecycleBeans > 1;
    }

}
