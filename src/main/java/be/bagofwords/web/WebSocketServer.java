/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-29. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.web;

import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.LifeCycleBean;
import be.bagofwords.ui.UI;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebSocketServer implements LifeCycleBean {

    private WebSocketImpl webSocketImpl;

    private final ApplicationContext applicationContext;
    private final int webSocketPort;
    private final List<WebSocketHandlerFactory> handlerFactories;
    private final Map<WebSocket, WebSocketHandler> handlersMap;
    private final Object mutex = new Object();

    public WebSocketServer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.webSocketPort = Integer.parseInt(applicationContext.getProperty("web.socket.port"));
        this.handlerFactories = new ArrayList<>();
        this.handlersMap = new HashMap<>();
    }

    @Override
    public void startBean() {
        registerControllers();
        webSocketImpl = new WebSocketImpl(new InetSocketAddress(webSocketPort));
        webSocketImpl.start();
        UI.write("Created websocket server on port " + webSocketPort);
    }

    private void registerControllers() {
        List<? extends WebSocketHandlerFactory> handlers = applicationContext.getBeans(WebSocketHandlerFactory.class);
        UI.write("Found " + handlers.size() + " websocket handlers");
        for (WebSocketHandlerFactory handler : handlers) {
            this.handlerFactories.add(handler);
        }
    }

    @Override
    public void stopBean() {
        if (webSocketImpl != null) {
            try {
                webSocketImpl.stop();
            } catch (IOException | InterruptedException e) {
                UI.writeError("Failed to stop websocket", e);
            }
        }
    }

    private class WebSocketImpl extends org.java_websocket.server.WebSocketServer {

        public WebSocketImpl(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
            UI.write("Websocket opened");
        }

        @Override
        public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
            UI.write("Closing websocket");
            removeHandler(webSocket);
        }

        private void removeHandler(WebSocket webSocket) {
            synchronized (mutex) {
                if (handlersMap.containsKey(webSocket)) {
                    WebSocketHandler handler = handlersMap.get(webSocket);
                    handler.onClose();
                    handlersMap.remove(webSocket);
                }
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, String message) {
            synchronized (mutex) {
                if (handlersMap.containsKey(webSocket)) {
                    try {
                        handlersMap.get(webSocket).onMessage(message);
                    } catch (Throwable t) {
                        UI.writeError("Unexpected error while handling websocket message", t);
                        webSocket.send("ERROR: " + t.getMessage());
                        webSocket.close();
                    }
                } else {
                    UI.write("Received protocol initiation " + message);
                    for (WebSocketHandlerFactory factory : handlerFactories) {
                        if (factory.getProtocolName().equals(message)) {
                            WebSocketHandler handler = factory.createHandler(webSocket);
                            applicationContext.wireBean(handler);
                            handlersMap.put(webSocket, handler);
                            handler.onOpen();
                            break;
                        }
                    }
                }

            }

        }

        @Override
        public void onError(WebSocket webSocket, Exception exception) {
            UI.writeError("Received websocket error ", exception);
            removeHandler(webSocket);
        }
    }
}
