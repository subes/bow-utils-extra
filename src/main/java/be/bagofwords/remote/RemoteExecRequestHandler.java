/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-29. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.remote;

import be.bagofwords.exec.*;
import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.remote.RemoteApplicationExec;
import be.bagofwords.util.SocketConnection;
import be.bagofwords.web.SocketRequestHandler;

public class RemoteExecRequestHandler extends SocketRequestHandler {

    private final ApplicationContext applicationContext;

    public RemoteExecRequestHandler(SocketConnection connection, ApplicationContext applicationContext) {
        super(connection);
        this.applicationContext = applicationContext;
    }

    @Override
    public void handleRequests() throws Exception {
        PackedRemoteObject packedRemoteExec = connection.readValue(PackedRemoteObject.class);
        try {
            ExecDataStream dataStream = new ExecDataStream(connection);
            RemoteLogFactory remoteLogFactory = new RemoteLogFactory(connection);
            RemoteApplicationExec executor = (RemoteApplicationExec) RemoteObjectUtil.loadObject(packedRemoteExec, remoteLogFactory);
            connection.writeValue(RemoteExecAction.IS_STARTED);
            executor.exec(dataStream, applicationContext);
        } catch (Exception exp) {
            Log.e("Failed to execute " + packedRemoteExec.objectClassName, exp);
            connection.writeError("Failed to execute " + packedRemoteExec.objectClassName, exp);
        } finally {
            connection.writeValue(RemoteExecAction.IS_FINISHED);
        }
    }

    @Override
    public void reportUnexpectedError(Exception ex) {
        Log.e("Unexpected error", ex);
    }

}
