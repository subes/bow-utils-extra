/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-7-29. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.events;

import be.bagofwords.exec.RemoteClass;
import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.remote.ExecDataStream;
import be.bagofwords.minidepi.remote.RemoteApplicationExec;

@RemoteClass
public class DummyRemoteExecutable implements RemoteApplicationExec {

    private final DummyRemoteExecutableDependencyClass dependencyClass;

    public DummyRemoteExecutable() {
        this.dependencyClass = new DummyRemoteExecutableDependencyClass();
    }

    @Override
    public void exec(ExecDataStream execDataStream, ApplicationContext applicationContext) throws Exception {
        Log.i("Hi from dummy!");
        dependencyClass.doSomething();
    }
}
