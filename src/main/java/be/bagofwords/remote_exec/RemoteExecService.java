/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.remote_exec;

import be.bagofwords.exec.RemoteExecConfig;
import be.bagofwords.util.SocketConnection;

import java.io.IOException;

public class RemoteExecService {

    public SocketConnection execRemotely(String host, int port, RemoteExecConfig remoteExecConfig) throws IOException {
        SocketConnection socketConnection = new SocketConnection(host, port, RemoteExecRequestHandlerFactory.SOCKET_NAME);
        socketConnection.writeValue(remoteExecConfig.pack());
        return socketConnection;
    }

}
