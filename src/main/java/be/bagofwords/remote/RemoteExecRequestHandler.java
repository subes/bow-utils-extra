/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-29. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.remote;

import be.bagofwords.exec.PackedRemoteObject;
import be.bagofwords.exec.RemoteExecAction;
import be.bagofwords.exec.RemoteObjectClassLoader;
import be.bagofwords.exec.RemoteObjectUtil;
import be.bagofwords.logging.FlexibleSl4jLogFactory;
import be.bagofwords.logging.Log;
import be.bagofwords.logging.RemoteLogger;
import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.remote.ExecDataStream;
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
            RemoteObjectClassLoader remoteObjectClassLoader = new RemoteObjectClassLoader(this.getClass().getClassLoader());
            remoteObjectClassLoader.addRemoteClasses(packedRemoteExec.classSources);
            ExecDataStream dataStream = new ExecDataStream(connection);
            RemoteApplicationExec executor = (RemoteApplicationExec) RemoteObjectUtil.loadObject(packedRemoteExec, remoteObjectClassLoader);
            ThreadGroup tg = new ThreadGroup("remote-exec-" + executor.getClass());
            Thread t = new Thread(tg, () -> {
                setName("remote-exec-" + executor.getClass());
                try {
                    connection.writeValue(RemoteExecAction.IS_STARTED);
                    executor.exec(dataStream, applicationContext);
                } catch (Exception exp) {
                    Log.e("Failed to execute " + executor, exp);
                }
            });
            FlexibleSl4jLogFactory.INSTANCE.setLoggerImplementation(t, new RemoteLogger(connection));
            t.start();
            t.join();
            tg.interrupt(); //Make sure all threads that have been started by this job are terminated.
        } catch (Exception exp) {
            Log.e("Failed to execute remote request", exp);
            connection.writeError("Failed to execute remote request", exp);
        } finally {
            connection.writeValue(RemoteExecAction.IS_FINISHED);
        }
    }

    @Override
    public void reportUnexpectedError(Exception ex) {
        Log.e("Unexpected error", ex);
    }

}
