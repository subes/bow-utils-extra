/*******************************************************************************
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-7-29. For license
 * information see the LICENSE file in the root folder of this repository.
 ******************************************************************************/

package be.bagofwords.minidepi.remote;

import be.bagofwords.exec.RemoteExecAction;
import be.bagofwords.logging.Log;
import be.bagofwords.util.SocketConnection;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

public class ExecDataStream {

    private final SocketConnection socketConnection;

    public ExecDataStream(SocketConnection socketConnection) {
        this.socketConnection = socketConnection;
    }

    public <T> T readValue(Class<T> _class, Class... genericParams) throws IOException {
        return socketConnection.readValue(_class, genericParams);
    }

    public void writeValue(Object value) {
        try {
            synchronized (socketConnection) {
                socketConnection.writeValue(RemoteExecAction.WRITE_VALUE);
                socketConnection.writeValue(value);
                socketConnection.flush();
            }
        } catch (IOException exp) {
            Log.w("Failed to write value, closing connection", exp);
            IOUtils.closeQuietly(socketConnection);
        }
    }

}
