/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.remote_exec;

import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.util.SocketConnection;
import be.bagofwords.web.SocketRequestHandler;
import be.bagofwords.web.SocketRequestHandlerFactory;

import java.io.IOException;

public class RemoteExecRequestHandlerFactory implements SocketRequestHandlerFactory {

    public static final String SOCKET_NAME = "remote-exec";

    @Inject
    private ApplicationContext applicationContext;

    @Override
    public String getName() {
        return SOCKET_NAME;
    }

    @Override
    public SocketRequestHandler createSocketRequestHandler(SocketConnection socketConnection) throws IOException {
        return new RemoteExecRequestHandler(socketConnection, applicationContext);
    }
}
