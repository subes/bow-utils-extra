/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.minidepi.events;

import be.bagofwords.exec.RemoteClass;
import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.remote.ExecDataStream;
import be.bagofwords.minidepi.remote.RemoteApplicationExec;

@RemoteClass
public class RemoteEventListener<T> implements EventListener<T>, RemoteApplicationExec {

    private final Class<T> eventClass;
    private final EventFilter<T> filter;

    public RemoteEventListener(Class<T> eventClass, EventFilter<T> filter) {
        this.eventClass = eventClass;
        this.filter = filter;
    }

    private ExecDataStream valueWriter;

    @Override
    public void exec(ExecDataStream dataStream, ApplicationContext applicationContext) throws Exception {
        EventService eventService = null;
        try {
            this.valueWriter = dataStream;
            eventService = applicationContext.getBean(EventService.class);
            eventService.registerListener(eventClass, this);
            this.valueWriter.readValue(Boolean.class);
        } catch (Exception exp) {
            eventService.removeListener(this);
        }
    }

    @Override
    public void handleEvent(T event) {
        if (filter.accept(event)) {
            synchronized (valueWriter) {
                valueWriter.writeValue(event);
            }
        }
    }
}
