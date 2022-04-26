/*******************************************************************************
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-7-29. For license
 * information see the LICENSE file in the root folder of this repository.
 ******************************************************************************/

package be.bagofwords.logging;

import be.bagofwords.exec.RemoteExecAction;
import be.bagofwords.exec.RemoteLogStatement;
import be.bagofwords.util.SocketConnection;
import be.bagofwords.util.Utils;

import static be.bagofwords.util.Utils.noException;

public class RemoteLogger implements LogImpl {

    private boolean closed = false;
    private final SocketConnection socketConnection;

    public RemoteLogger(SocketConnection socketConnection) {
        this.socketConnection = socketConnection;
    }

    @Override
    public void log(int level, String logger, String message, Throwable throwable) {
        if (!closed) {
            String[] lines;
            if (throwable != null) {
                lines = Utils.getStackTrace(throwable).split("\n");
            } else {
                lines = null;
            }
            final RemoteLogStatement remoteLogStatement = new RemoteLogStatement(FlexibleSlf4jLogger.toLevel(level), logger, message, lines);
            synchronized (socketConnection) {
                noException(new Utils.Action() {
                    @Override
                    public void run() throws Exception {
                        socketConnection.writeValue(RemoteExecAction.SEND_LOG);
                        socketConnection.writeValue(remoteLogStatement);
                        socketConnection.flush();
                    }
                });
            }
        }
    }

    @Override
    public void finalize() {
        this.closed = true;
    }

}
