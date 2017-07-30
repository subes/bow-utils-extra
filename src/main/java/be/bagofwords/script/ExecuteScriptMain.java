/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-7-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.script;

import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.ApplicationManager;
import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.minidepi.annotations.Property;
import be.bagofwords.remote.RemoteExecRequestHandlerFactory;
import be.bagofwords.web.SocketServer;

public class ExecuteScriptMain implements Runnable {

    public static void main(String[] args) {
        ApplicationManager.run(ExecuteScriptMain.class);
    }

    @Inject
    private RemoteExecRequestHandlerFactory remoteExecRequestHandlerFactory;

    @Inject
    private ApplicationContext applicationContext;

    @Property(value = "execute.script.port", orFrom = "bow-utils-extra.properties")
    private int executeScriptPort;

    @Override
    public void run() {
        applicationContext.setProperty("socket.port", Integer.toString(executeScriptPort)); // Start socket server on correct port
        applicationContext.getBean(SocketServer.class);
        applicationContext.waitUntilTerminated();
    }
}
