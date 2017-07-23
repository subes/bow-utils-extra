/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-29. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.remote;

import be.bagofwords.exec.PackedRemoteObject;
import be.bagofwords.exec.RemoteObjectUtil;
import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.remote.RemoteApplicationExec;
import be.bagofwords.util.SocketConnection;
import be.bagofwords.util.Utils;
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
            RemoteApplicationExec executor = (RemoteApplicationExec) RemoteObjectUtil.loadObject(packedRemoteExec);
            connection.writeBoolean(true);
            connection.readBoolean();
            executor.exec(connection, applicationContext);
        } catch (Exception exp) {
            connection.writeBoolean(false);
            connection.writeString(Utils.getStackTrace(exp));
            connection.readBoolean();
            Log.e("Failed to execute " + packedRemoteExec.objectClassName, exp);
        }
    }

    @Override
    public void reportUnexpectedError(Exception ex) {
        Log.e("Unexpected error", ex);
    }

}
