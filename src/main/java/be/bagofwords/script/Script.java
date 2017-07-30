/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-7-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.script;

import be.bagofwords.exec.RemoteClass;

import java.io.Serializable;

@RemoteClass
public interface Script extends Serializable {

    void execute(Memory memory) throws Exception;

}
