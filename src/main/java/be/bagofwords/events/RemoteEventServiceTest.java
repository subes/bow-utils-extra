/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.events;

import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.LifeCycleBean;
import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.remote_exec.RemoteExecRequestHandlerFactory;
import be.bagofwords.web.SocketServer;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class RemoteEventServiceTest {

    private static final int TEST_PORT = 8547;

    @Test
    public void testAll() {
        String eventMessage = "Hello there!";
        Map<String, String> serverConfig = new HashMap<>();
        serverConfig.put("socket.port", Integer.toString(TEST_PORT));
        ApplicationContext serverContext = new ApplicationContext(serverConfig);
        Server server = serverContext.getBean(Server.class);
        ApplicationContext clientContext = new ApplicationContext();
        Client client = clientContext.getBean(Client.class);
        server.generateEvent(eventMessage);
        clientContext.waitUntilTerminated();
        Assert.assertEquals(eventMessage, client.receivedMessage);
    }

    public static class Server {
        @Inject
        private EventService eventService;
        @Inject
        private RemoteExecRequestHandlerFactory remoteExecRequestHandlerFactory;
        @Inject
        private SocketServer socketServer;

        public void generateEvent(String message) {
            Event event = new Event();
            event.message = message;
            eventService.emitEvent(event);
        }
    }

    public static class Client implements EventListener<Event>, LifeCycleBean {
        @Inject
        private RemoteEventService remoteEventService;
        @Inject
        private ApplicationContext applicationContext;

        private String receivedMessage;

        @Override
        public void handleEvent(Event event) {
            receivedMessage = event.message;
            applicationContext.terminate();
        }

        @Override
        public void startBean() {
            remoteEventService.listenToEvents("localhost", TEST_PORT, Event.class, new TestEventFilter(), this);
        }

        @Override
        public void stopBean() {
            remoteEventService.removeListener(this);
        }

    }

    public static class TestEventFilter implements EventFilter<Event> {

        @Override
        public boolean accept(Event event) {
            return true;
        }
    }

    public static class Event {
        public String message;
    }

}