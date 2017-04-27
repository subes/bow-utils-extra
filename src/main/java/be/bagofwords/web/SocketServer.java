/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-21. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.web;

import be.bagofwords.application.status.StatusViewable;
import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.LifeCycleBean;
import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.ui.UI;
import be.bagofwords.util.NumUtils;
import be.bagofwords.util.SocketConnection;
import be.bagofwords.util.StringUtils;
import be.bagofwords.util.Utils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class SocketServer implements StatusViewable, LifeCycleBean {

    public static final String ENCODING = "UTF-8";

    @Inject
    private ApplicationContext applicationContext;

    private final Map<String, SocketRequestHandlerFactory> socketRequestHandlerFactories;
    private final List<SocketRequestHandler> runningRequestHandlers;

    private int totalNumberOfConnections;
    private ServerSocket serverSocket;

    public SocketServer() {
        this.runningRequestHandlers = new ArrayList<>();
        this.totalNumberOfConnections = 0;
        this.socketRequestHandlerFactories = new HashMap<>();
    }

    @Override
    public void startBean() {
        List<SocketRequestHandlerFactory> factories = applicationContext.getBeans(SocketRequestHandlerFactory.class);
        Log.i("Found " + factories.size() + " socket request handler factories");
        for (SocketRequestHandlerFactory factory : factories) {
            registerSocketRequestHandlerFactory(factory);
            applicationContext.registerRuntimeDependency(this, factory);
        }
        int port = Integer.parseInt(applicationContext.getProperty("socket.port", "bow-utils-extra.properties"));
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException exp) {
            this.serverSocket = null;
            throw new RuntimeException("Failed to initialize socket server on port " + port, exp);
        }
        new HandlerThread().start();
        Log.i("Started socket server on port " + port);
    }

    public synchronized void registerSocketRequestHandlerFactory(SocketRequestHandlerFactory factory) {
        if (socketRequestHandlerFactories.containsKey(factory.getName())) {
            throw new RuntimeException("A SocketRequestHandlerFactory was already registered with name " + factory.getName());
        }
        socketRequestHandlerFactories.put(factory.getName(), factory);
    }

    @Override
    public void stopBean() {
        IOUtils.closeQuietly(serverSocket);
        //once a request handler is finished, it removes itself from the list of requestHandlers, so we just wait until this list is empty
        while (!runningRequestHandlers.isEmpty()) {
            synchronized (runningRequestHandlers) {
                for (SocketRequestHandler requestHandler : runningRequestHandlers) {
                    requestHandler.interrupt();
                }
            }
            Utils.threadSleep(10);
        }
        Log.i("Socket server has terminated.");
    }

    public int getTotalNumberOfConnections() {
        return totalNumberOfConnections;
    }

    public List<SocketRequestHandler> getRunningRequestHandlers() {
        return runningRequestHandlers;
    }

    public void removeHandler(SocketRequestHandler handler) {
        synchronized (runningRequestHandlers) {
            runningRequestHandlers.remove(handler);
        }
    }

    @Override
    public void printHtmlStatus(StringBuilder sb) {
        sb.append("<h1>Printing database server statistics</h1>");
        ln(sb, "<table>");
        ln(sb, "<tr><td>Used memory is </td><td>" + UI.getMemoryUsage() + "</td></tr>");
        ln(sb, "<tr><td>Total number of connections </td><td>" + getTotalNumberOfConnections() + "</td></tr>");
        List<SocketRequestHandler> runningRequestHandlers = getRunningRequestHandlers();
        ln(sb, "<tr><td>Current number of handlers </td><td>" + runningRequestHandlers.size() + "</td></tr>");
        List<SocketRequestHandler> sortedRequestHandlers;
        synchronized (runningRequestHandlers) {
            sortedRequestHandlers = new ArrayList<>(runningRequestHandlers);
        }
        Collections.sort(sortedRequestHandlers, new Comparator<SocketRequestHandler>() {
            @Override
            public int compare(SocketRequestHandler o1, SocketRequestHandler o2) {
                return -Double.compare(o1.getTotalNumberOfRequests(), o2.getTotalNumberOfRequests());
            }
        });
        for (int i = 0; i < sortedRequestHandlers.size(); i++) {
            SocketRequestHandler handler = sortedRequestHandlers.get(i);
            ln(sb, "<tr><td>" + i + " Name </td><td>" + handler.getName() + "</td></tr>");
            ln(sb, "<tr><td>" + i + " Started at </td><td>" + new Date(handler.getStartTime()) + "</td></tr>");
            ln(sb, "<tr><td>" + i + " Total number of requests </td><td>" + handler.getTotalNumberOfRequests() + "</td></tr>");
            double requestsPerSec = handler.getTotalNumberOfRequests() * 1000.0 / (System.currentTimeMillis() - handler.getStartTime());
            ln(sb, "<tr><td>" + i + " Average requests/s</td><td>" + NumUtils.fmt(requestsPerSec) + "</td></tr>");
        }
        ln(sb, "</table>");
    }

    private void ln(StringBuilder sb, String s) {
        sb.append(s);
        sb.append("\n");
    }

    private class HandlerThread extends Thread {
        public HandlerThread() {
            super("socket-server-accept-thread");
        }

        public void run() {
            while (!serverSocket.isClosed() && !applicationContext.terminateWasRequested()) {
                try {
                    Socket acceptedSocket = serverSocket.accept();
                    SocketConnection connection = new SocketConnection(acceptedSocket);
                    String factoryName = connection.readString();
                    if (factoryName == null || StringUtils.isEmpty(factoryName.trim())) {
                        connection.writeError("No name specified for the requested SocketRequestHandlerFactory");
                        continue;
                    }
                    SocketRequestHandlerFactory factory = socketRequestHandlerFactories.get(factoryName);
                    if (factory == null) {
                        Log.w("No SocketRequestHandlerFactory registered for name " + factoryName);
                        connection.writeError("No SocketRequestHandlerFactory registered for name " + factoryName);
                        continue;
                    }
                    SocketRequestHandler handler = factory.createSocketRequestHandler(connection);
                    if (handler != null) {
                        handler.setName(factoryName + "_" + Long.toHexString(System.currentTimeMillis()));
                        handler.setSocketServer(SocketServer.this);
                        if (applicationContext.hasWiredFields(handler)) {
                            throw new RuntimeException("Handler " + handler + " has wired fields. This is not supported. You can wire the fields of the factory.");
                        }
                        synchronized (runningRequestHandlers) {
                            runningRequestHandlers.add(handler);
                        }
                        handler.start();
                        totalNumberOfConnections++;
                    } else {
                        Log.w("Factory " + factoryName + " failed to create a socket handler. Closing socket...");
                        acceptedSocket.close();
                    }
                } catch (IOException e) {
                    if (!(e instanceof SocketException || applicationContext.terminateWasRequested())) {
                        Log.e("Unexpected error in SocketServer HandlerThread", e);
                    }
                }
            }
        }
    }
}
