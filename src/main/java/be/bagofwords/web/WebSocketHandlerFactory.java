/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-29. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.web;

import org.java_websocket.WebSocket;

public interface WebSocketHandlerFactory {

    String getProtocolName();

    WebSocketHandler createHandler(WebSocket webSocket);

}
