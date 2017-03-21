/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-21. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.web;

import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.ui.UI;
import be.bagofwords.util.SocketConnection;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

public abstract class SocketRequestHandler extends Thread {

    protected SocketConnection connection;
    private long startTime;

    @Inject
    private SocketServer socketServer;

    public SocketRequestHandler(SocketConnection connection) {
        setDaemon(true);
        this.connection = connection;
    }

    public long getStartTime() {
        return startTime;
    }

    @Override
    public void run() {
        try {
            this.connection.ensureBuffered();
            startTime = System.currentTimeMillis();
            handleRequests();
        } catch (Exception ex) {
            if (isUnexpectedError(ex)) {
                try {
                    connection.writeError("Unexpected error", ex);
                } catch (IOException e) {
                    UI.writeError("Failed to send unexpected error on socket", e);
                }
                reportUnexpectedError(ex);
            }
        }
        IOUtils.closeQuietly(connection);
        socketServer.removeHandler(this);
    }

    protected boolean isUnexpectedError(Exception ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("Connection reset")) {
            return false;
        }
        for (StackTraceElement el : ex.getStackTrace()) {
            if (el.getMethodName().equals("readNextAction")) {
                return false;
            }
        }
        return true;
    }

    public abstract void handleRequests() throws Exception;

    public abstract void reportUnexpectedError(Exception ex);

    public long getTotalNumberOfRequests() {
        return -1; //Should be overridden in subclasses
    }

    @Override
    public void interrupt() {
        IOUtils.closeQuietly(connection);
        super.interrupt();
    }
}
