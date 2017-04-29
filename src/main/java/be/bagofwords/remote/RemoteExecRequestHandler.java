/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-29. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.remote;

import be.bagofwords.exec.PackedRemoteExec;
import be.bagofwords.exec.RemoteExecUtil;
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
        PackedRemoteExec packedRemoteExec = connection.readValue(PackedRemoteExec.class);
        try {
            RemoteApplicationExec executor = (RemoteApplicationExec) RemoteExecUtil.loadRemoteRunner(packedRemoteExec);
            executor.exec(connection, applicationContext);
        } catch (Exception exp) {
            Log.e("Failed to execute " + packedRemoteExec.executorClassName, exp);
        }
    }

    @Override
    public void reportUnexpectedError(Exception ex) {
        Log.e("Unexpected error", ex);
    }

}
