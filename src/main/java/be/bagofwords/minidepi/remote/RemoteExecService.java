/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.minidepi.remote;

import be.bagofwords.exec.RemoteExecAction;
import be.bagofwords.exec.RemoteLogStatement;
import be.bagofwords.exec.RemoteObjectConfig;
import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.LifeCycleBean;
import be.bagofwords.util.SocketConnection;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RemoteExecService implements LifeCycleBean {

    public static final String SOCKET_NAME = "remote-exec";
    private final List<SocketConnection> connections = new ArrayList<>();

    public void execRemotely(String host, int port, RemoteApplicationExec exec, Class... extraClasses) throws IOException {
        RemoteObjectConfig config = RemoteObjectConfig.create(exec);
        for (Class extraClass : extraClasses) {
            config.add(extraClass);
        }
        execRemotely(host, port, config);
    }

    public void execRemotely(String host, int port, RemoteObjectConfig remoteObjectConfig) throws IOException {
        execRemotely(host, port, remoteObjectConfig, new RemoteExecEventHandler() {
            @Override
            public void started(SocketConnection socketConnection) {
                //Ok
            }

            @Override
            public void finished() {
                //Ok
            }

            @Override
            public void handleValue(SocketConnection socketConnection) throws IOException {
                throw new RuntimeException("No custom values expected");
            }
        });
    }

    public void execRemotely(String host, int port, RemoteObjectConfig remoteObjectConfig, RemoteExecEventHandler remoteExecEventHandler) throws IOException {
        SocketConnection socketConnection = new SocketConnection(host, port, SOCKET_NAME);
        synchronized (connections) {
            connections.add(socketConnection);
        }
        try {
            socketConnection.writeValue(remoteObjectConfig.pack());
            boolean finished = false;
            while (!finished) {
                RemoteExecAction action = socketConnection.readValue(RemoteExecAction.class);
                if (action == RemoteExecAction.IS_STARTED) {
                    remoteExecEventHandler.started(socketConnection);
                } else if (action == RemoteExecAction.IS_FINISHED) {
                    remoteExecEventHandler.finished();
                    finished = true;
                } else if (action == RemoteExecAction.SEND_LOG) {
                    RemoteLogStatement logStatement = socketConnection.readValue(RemoteLogStatement.class);
                    synchronized (Log.LOCK) {
                        Log.log(logStatement.level, "REMOTE " + logStatement.logger, logStatement.message, null);
                        if (logStatement.stackTrace != null) {
                            for (String stackTrace : logStatement.stackTrace) {
                                Log.log(logStatement.level, "REMOTE " + logStatement.logger, stackTrace, null);
                            }
                        }
                    }
                } else if (action == RemoteExecAction.WRITE_VALUE) {
                    remoteExecEventHandler.handleValue(socketConnection);
                } else {
                    throw new RuntimeException("Unexpected action " + action);
                }
            }
        } finally {
            IOUtils.closeQuietly(socketConnection);
        }
    }

    @Override
    public void startBean() {

    }

    @Override
    public void stopBean() {
        synchronized (connections) {
            for (SocketConnection connection : connections) {
                IOUtils.closeQuietly(connection);
            }
        }
    }
}
