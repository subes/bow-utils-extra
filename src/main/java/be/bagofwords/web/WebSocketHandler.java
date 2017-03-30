/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-29. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.web;

import org.java_websocket.WebSocket;

public abstract class WebSocketHandler {

    protected final WebSocket webSocket;

    public WebSocketHandler(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public abstract void onOpen();

    public abstract void onMessage(String message);

    public abstract void onClose();

}
