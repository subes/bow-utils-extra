/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.remote_exec;

import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.util.SocketConnection;

import java.io.Serializable;

public interface RemoteApplicationExec extends Serializable {

    void exec(SocketConnection socketConnection, ApplicationContext applicationContext) throws Exception;

}
