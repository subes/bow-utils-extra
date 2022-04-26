/*******************************************************************************
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-7-29. For license
 * information see the LICENSE file in the root folder of this repository.
 ******************************************************************************/

package be.bagofwords.minidepi.remote;

import be.bagofwords.util.SocketConnection;

import java.io.IOException;

public interface RemoteExecEventHandler {

    void started(SocketConnection socketConnection);

    void finished();

    void handleValue(SocketConnection socketConnection) throws IOException;

}
