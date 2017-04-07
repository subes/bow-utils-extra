/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-21. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.web;

import be.bagofwords.util.SocketConnection;

import java.io.IOException;

public interface SocketRequestHandlerFactory {

    String getName();

    SocketRequestHandler createSocketRequestHandler(SocketConnection socketConnection) throws IOException;

}
