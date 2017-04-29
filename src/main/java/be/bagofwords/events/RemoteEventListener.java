/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.events;

import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.remote_exec.RemoteApplicationExec;
import be.bagofwords.util.SocketConnection;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

public class RemoteEventListener<T> implements EventListener<T>, RemoteApplicationExec {

    private final Class<T> eventClass;
    private final EventFilter<T> filter;

    public RemoteEventListener(Class<T> eventClass, EventFilter<T> filter) {
        this.eventClass = eventClass;
        this.filter = filter;
    }

    private SocketConnection socketConnection;

    @Override
    public void exec(SocketConnection socketConn, ApplicationContext applicationContext) throws Exception {
        EventService eventService = null;
        try {
            socketConnection = socketConn;
            eventService = applicationContext.getBean(EventService.class);
            eventService.registerListener(eventClass, this);
            synchronized (socketConnection) {
                socketConnection.writeBoolean(true);
            }
            socketConnection.readBoolean(); //Boolean will be send by client when finished
        } catch (Exception exp) {
            eventService.removeListener(this);
        }
    }

    @Override
    public void handleEvent(T event) {
        if (filter.accept(event)) {
            synchronized (socketConnection) {
                try {
                    socketConnection.writeValue(event);
                    socketConnection.flush();
                } catch (IOException e) {
                    Log.i("Failed to send event", e);
                    IOUtils.closeQuietly(socketConnection);
                }
            }
        }
    }
}
