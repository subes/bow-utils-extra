/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.events;

import be.bagofwords.exec.RemoteExecConfig;
import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.remote_exec.RemoteExecService;
import be.bagofwords.util.SafeThread;
import be.bagofwords.util.SocketConnection;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RemoteEventService {

    @Inject
    private RemoteExecService remoteExecService;

    private final Map<EventListener, RemoteEventServiceThread> threads = new HashMap<>();

    public <T> void listenToEvents(String host, int port, Class<T> eventClass, EventFilter<T> filter, EventListener<T> localListener) {
        RemoteEventServiceThread<T> thread = new RemoteEventServiceThread<>(host, port, eventClass, localListener, filter);
        synchronized (threads) {
            threads.put(localListener, thread);
        }
        thread.start();
        waitUntilThreadIsActuallyListening(thread);
    }

    private void waitUntilThreadIsActuallyListening(RemoteEventServiceThread thread) {
        try {
            synchronized (thread) {
                thread.wait(10 * 1000);
            }
            if (!thread.isListening) {
                Log.w("Failed to listen for remove events. Possible connection problem?");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for thread " + thread + " to start", e);
        }
    }

    public void removeListener(EventListener listener) {
        synchronized (threads) {
            threads.remove(listener);
        }
    }

    public class RemoteEventServiceThread<T> extends SafeThread {

        private final String host;
        private final int port;
        private final Class<T> eventClass;
        private final EventListener<T> listener;
        private final EventFilter<T> filter;

        private int numOfConsecutiveFailures = 0;
        private SocketConnection socketConnection;
        public boolean isListening;

        public RemoteEventServiceThread(String host, int port, Class<T> eventClass, EventListener<T> listener, EventFilter filter) {
            super("remote-events-thread", true);
            this.host = host;
            this.port = port;
            this.eventClass = eventClass;
            this.listener = listener;
            this.filter = filter;
        }

        @Override
        protected void runImpl() throws Exception {
            RemoteEventListener<T> remoteEventListener = new RemoteEventListener<>(eventClass, filter);
            RemoteExecConfig execConfig = RemoteExecConfig.create(remoteEventListener).add(filter.getClass());
            while (!isTerminateRequested()) {
                try {
                    socketConnection = remoteExecService.execRemotely(host, port, execConfig);
                    socketConnection.readBoolean();
                    synchronized (this) {
                        this.isListening = true;
                        this.notifyAll(); //Necessary for waitUntilThreadIsActuallyListening(..) method
                    }
                    while (!isTerminateRequested()) {
                        T event = socketConnection.readValue(eventClass);
                        listener.handleEvent(event);
                        numOfConsecutiveFailures = 0;
                    }
                } catch (IOException exp) {
                    IOUtils.closeQuietly(socketConnection);
                    numOfConsecutiveFailures++;
                    int secondsToWait = Math.min(10, numOfConsecutiveFailures);
                    Log.i("Error while receiving events from " + host + ":" + port + ". Will reconnect in " + secondsToWait + "s", exp);
                    try {
                        Thread.sleep(secondsToWait * 1000);
                    } catch (InterruptedException intExp) {
                        //OK
                    }
                }
            }
        }

        @Override
        public void doTerminate() {
            if (socketConnection != null) {
                try {
                    socketConnection.writeBoolean(true);
                } catch (IOException e) {
                    //Ah well
                }
                IOUtils.closeQuietly(socketConnection);
            }
            synchronized (threads) {
                threads.remove(listener);
            }
        }
    }

}
