/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.minidepi.events;

import be.bagofwords.exec.ClassSourceReader;
import be.bagofwords.exec.RemoteObjectConfig;
import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.minidepi.remote.RemoteExecEventHandler;
import be.bagofwords.minidepi.remote.RemoteExecService;
import be.bagofwords.minidepi.services.TimeService;
import be.bagofwords.util.SafeThread;
import be.bagofwords.util.SocketConnection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RemoteEventService {

    @Inject
    private RemoteExecService remoteExecService;
    @Inject
    private TimeService timeService;

    private final Map<EventListener, RemoteEventServiceThread> threads = new HashMap<>();

    public <T> void listenToEvents(String host, int port, Class<T> eventClass, EventFilter<T> filter, EventListener<T> localListener) {
        listenToEvents(host, port, eventClass, filter, localListener, null);
    }

    public <T> void listenToEvents(String host, int port, Class<T> eventClass, EventFilter<T> filter, EventListener<T> localListener, ClassSourceReader classSourceReader) {
        RemoteEventServiceThread<T> thread = new RemoteEventServiceThread<>(host, port, eventClass, localListener, filter, classSourceReader);
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
                Log.w("Failed to listen for remote events. Possible connection problem?");
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
        private final ClassSourceReader classSourceReader;

        private int numOfConsecutiveFailures = 0;
        private SocketConnection socketConnection;
        public boolean isListening;

        public RemoteEventServiceThread(String host, int port, Class<T> eventClass, EventListener<T> listener, EventFilter<T> filter, ClassSourceReader classSourceReader) {
            super("remote-events-thread", true);
            this.host = host;
            this.port = port;
            this.eventClass = eventClass;
            this.listener = listener;
            this.filter = filter;
            this.classSourceReader = classSourceReader;
        }

        @Override
        protected void runImpl() throws Exception {
            RemoteEventListener<T> remoteEventListener = new RemoteEventListener<>(eventClass, filter);
            RemoteObjectConfig remoteConfig = RemoteObjectConfig.create(remoteEventListener).add(filter.getClass());
            if (classSourceReader != null) {
                remoteConfig = remoteConfig.sourceReader(classSourceReader);
            }
            while (!isTerminateRequested()) {
                try {
                    remoteExecService.execRemotely(host, port, remoteConfig, new RemoteExecEventHandler() {
                        @Override
                        public void started(SocketConnection socketConnection) {
                            synchronized (RemoteEventServiceThread.this) {
                                RemoteEventServiceThread.this.socketConnection = socketConnection;
                                RemoteEventServiceThread.this.isListening = true;
                                RemoteEventServiceThread.this.notifyAll(); //Necessary for waitUntilThreadIsActuallyListening(..) method
                            }
                        }

                        @Override
                        public void finished() {
                            //Do nothing
                        }

                        @Override
                        public void handleValue(SocketConnection socketConnection) throws IOException {
                            T event = socketConnection.readValue(eventClass);
                            listener.handleEvent(event);
                            numOfConsecutiveFailures = 0;
                        }
                    });
                } catch (IOException exp) {
                    RemoteEventServiceThread.this.notifyAll(); //Necessary for waitUntilThreadIsActuallyListening(..) method
                    numOfConsecutiveFailures++;
                    int secondsToWait = Math.min(10, numOfConsecutiveFailures);
                    Log.i("Error while receiving events from " + host + ":" + port + ". Will reconnect in " + secondsToWait + "s", exp);
                    try {
                        timeService.sleep(secondsToWait * 1000);
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
                    socketConnection.writeValue(Boolean.TRUE);
                } catch (IOException e) {
                    //Ah well
                }
                org.apache.commons.io.IOUtils.closeQuietly(socketConnection);
            }
            synchronized (threads) {
                threads.remove(listener);
            }
        }
    }

}
