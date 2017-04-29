/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-29. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.remote;

import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.minidepi.remote.RemoteExecService;
import be.bagofwords.util.SocketConnection;
import be.bagofwords.web.SocketRequestHandler;
import be.bagofwords.web.SocketRequestHandlerFactory;

import java.io.IOException;

public class RemoteExecRequestHandlerFactory implements SocketRequestHandlerFactory {

    @Inject
    private ApplicationContext applicationContext;

    @Override
    public String getName() {
        return RemoteExecService.SOCKET_NAME;
    }

    @Override
    public SocketRequestHandler createSocketRequestHandler(SocketConnection socketConnection) throws IOException {
        return new RemoteExecRequestHandler(socketConnection, applicationContext);
    }
}
