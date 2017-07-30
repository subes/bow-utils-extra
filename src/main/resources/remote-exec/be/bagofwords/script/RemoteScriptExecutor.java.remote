/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-7-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.script;

import be.bagofwords.exec.ExecDataStream;
import be.bagofwords.exec.RemoteClass;
import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.remote.RemoteApplicationExec;

@RemoteClass
public class RemoteScriptExecutor implements RemoteApplicationExec {

    private final Script script;

    public RemoteScriptExecutor(Script script) {
        this.script = script;
    }

    @Override
    public void exec(ExecDataStream execDataStream, ApplicationContext applicationContext) throws Exception {
        Memory memory = applicationContext.getBean(Memory.class);
        script.execute(memory);
    }
}
