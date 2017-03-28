package be.bagofwords.events;

import be.bagofwords.minidepi.LifeCycleBean;
import be.bagofwords.ui.UI;
import be.bagofwords.util.ConcurrencyUtils;
import be.bagofwords.util.MappedLists;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by koen on 4/03/17.
 */
public class EventService implements LifeCycleBean {

    private final MappedLists<String, EventListener> registeredListeners = new MappedLists<>();
    private ExecutorService executorService;

    public <T> void registerListener(String type, EventListener<T> listener) {
        synchronized (registeredListeners) {
            registeredListeners.get(type).add(listener);
        }
    }

    public void removeListener(EventListener listener) {
        synchronized (registeredListeners) {
            for (List<EventListener> eventListeners : registeredListeners.values()) {
                eventListeners.remove(listener);
            }
        }
    }

    public <T> void messageReceived(String type, T event) {
        executorService.submit(() -> {
            //Perform handling of the event outside of synchronized block
            List<EventListener<T>> eventListeners = new ArrayList<>();
            synchronized (registeredListeners) {
                if (registeredListeners.containsKey(type)) {
                    for (EventListener listener : registeredListeners.get(type)) {
                        eventListeners.add(listener);
                    }
                }
            }
            for (EventListener<T> listener : eventListeners) {
                try {
                    listener.handleEvent(event);
                } catch (Exception exp) {
                    UI.writeError("Error while handling event " + event + " by handler " + listener, exp);
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
