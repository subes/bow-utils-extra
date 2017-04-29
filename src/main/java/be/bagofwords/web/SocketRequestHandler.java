/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-21. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.web;

import be.bagofwords.logging.Log;
import be.bagofwords.util.SocketConnection;
import org.apache.commons.io.IOUtils;

import java.io.EOFException;
import java.io.IOException;

public abstract class SocketRequestHandler extends Thread {

    protected SocketConnection connection;
    private long startTime;
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
            setUp();
            this.connection.ensureBuffered();
            startTime = System.currentTimeMillis();
            handleRequests();
        } catch (Exception ex) {
            if (isUnexpectedError(ex)) {
                try {
                    connection.writeError("Unexpected error", ex);
                } catch (IOException e) {
                    Log.e("Failed to send unexpected error on socket", e);
                }
                reportUnexpectedError(ex);
            }
        }
        try {
            tearDown();
        } catch (Exception exp) {
            Log.e("Error while executing tearDown() method of " + this, exp);
        }
        socketServer.removeHandler(this);
    }

    protected boolean isUnexpectedError(Exception ex) {
        if (ex instanceof EOFException) {
            return false;
        }
        String message = ex.getMessage();
        if (message != null && (message.contains("Connection reset") || message.contains("Stream closed") || message.contains("Socket closed"))) {
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

    public void setUp() throws Exception {
        //Default implementation is empty
    }

    public void tearDown() throws Exception {
        IOUtils.closeQuietly(connection);
    }

    public abstract void reportUnexpectedError(Exception ex);

    public long getTotalNumberOfRequests() {
        return -1; //Should be overridden in subclasses
    }

    @Override
    public void interrupt() {
        IOUtils.closeQuietly(connection);
        super.interrupt();
    }

    public void setSocketServer(SocketServer socketServer) {
        this.socketServer = socketServer;
    }
}
