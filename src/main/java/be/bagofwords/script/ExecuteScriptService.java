/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-7-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.script;

import be.bagofwords.exec.RemoteObjectConfig;
import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.minidepi.annotations.Property;
import be.bagofwords.minidepi.remote.RemoteExecService;

import java.io.IOException;

public class ExecuteScriptService {

    public static void executeScript(Script script, Class... extraClasses) {
        ApplicationContext applicationContext = new ApplicationContext();
        try {
            ExecuteScriptService service = applicationContext.getBean(ExecuteScriptService.class);
            service.doExecuteScript(script, extraClasses);
        } catch (IOException exp) {
            throw new RuntimeException("Failed to execute script " + script, exp);
        } finally {
            applicationContext.terminate();
        }
    }

    @Property(value = "execute.script.port", orFrom = "bow-utils-extra.properties")
    private int executeScriptPort;

    @Inject
    private RemoteExecService remoteExecService;

    public void doExecuteScript(Script script, Class... extraClasses) throws IOException {
        RemoteScriptExecutor remoteScriptExecutor = new RemoteScriptExecutor(script);
        RemoteObjectConfig config = RemoteObjectConfig.create(remoteScriptExecutor).add(script.getClass()).add(Script.class);
        for (Class extraClass : extraClasses) {
            config.add(extraClass);
        }
        remoteExecService.execRemotely("localhost", executeScriptPort, config);
    }

}
