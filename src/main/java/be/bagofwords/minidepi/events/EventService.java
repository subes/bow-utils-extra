package be.bagofwords.minidepi.events;

import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.LifeCycleBean;
import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.util.ConcurrencyUtils;
import be.bagofwords.util.MappedLists;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Created by koen on 4/03/17.
 */
public class EventService implements LifeCycleBean {

    @Inject
    private ApplicationContext applicationContext;

    private final MappedLists<Class, EventListener> registeredListeners = new MappedLists<>();
    private ExecutorService executorService;

    public <T> void registerListener(Class<T> _type, EventListener<T> listener) {
        synchronized (registeredListeners) {
            registeredListeners.get(_type).add(listener);
        }
    }

    public void removeListener(EventListener listener) {
        synchronized (registeredListeners) {
            for (List<EventListener> eventListeners : registeredListeners.values()) {
                eventListeners.remove(listener);
            }
        }
    }

    public <T> void emitEvent(final T event) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                //Perform handling of the event outside of synchronized block
                List<EventListener<T>> eventListeners = new ArrayList<>();
                synchronized (registeredListeners) {
                    for (Map.Entry<Class, List<EventListener>> entry : registeredListeners.entrySet()) {
                        if (entry.getKey().isInstance(event)) {
                            for (EventListener listener : entry.getValue()) {
                                eventListeners.add(listener);
                            }
                        }
                    }
                }
                for (EventListener<T> listener : eventListeners) {
                    try {
                        listener.handleEvent(event);
                    } catch (Exception exp) {
                        Log.e("Error while handling event " + event + " by handler " + listener, exp);
                    }
                }
            }
        });
    }

    @Override
    public void startBean() {
        executorService = ConcurrencyUtils.multiThreadedExecutor("event-service");
    }

    @Override
    public void stopBean() {
        synchronized (registeredListeners) {
            registeredListeners.clear();
        }
        ConcurrencyUtils.terminateAndWaitForExecutorService(executorService, "event service", 10);
    }
}
