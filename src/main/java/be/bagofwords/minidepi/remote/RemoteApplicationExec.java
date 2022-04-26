/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.minidepi.remote;

import be.bagofwords.minidepi.ApplicationContext;

import java.io.Serializable;

public interface RemoteApplicationExec extends Serializable {

    void exec(ExecDataStream dataStream, ApplicationContext applicationContext) throws Exception;

}
