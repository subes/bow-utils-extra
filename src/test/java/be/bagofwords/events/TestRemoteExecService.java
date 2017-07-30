/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-7-29. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.events;

import be.bagofwords.exec.RemoteObjectConfig;
import be.bagofwords.minidepi.ApplicationContext;
import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.minidepi.remote.RemoteExecService;
import be.bagofwords.remote.RemoteExecRequestHandlerFactory;
import be.bagofwords.web.SocketServer;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TestRemoteExecService {

    private static final int TEST_PORT = 8547;
    public static final String TEST_FILE = "/tmp/testRemoteExecService.txt";

    @Test
    public void testRemoteExec() throws IOException {
        File testFile = new File(TestRemoteExecService.TEST_FILE);
        if (testFile.exists() && !testFile.delete()) {
            throw new RuntimeException("Failed to clean " + testFile.getAbsolutePath());
        }

        Map<String, String> serverConfig = new HashMap<>();
        serverConfig.put("socket.port", Integer.toString(TEST_PORT));
        ApplicationContext serverContext = new ApplicationContext(serverConfig);
        serverContext.getBean(Server.class);
        ApplicationContext clientContext = new ApplicationContext();
        Client client = clientContext.getBean(Client.class);

        DummyRemoteExecutable executable = new DummyRemoteExecutable();
        RemoteObjectConfig config = RemoteObjectConfig.create(executable).add(DummyRemoteExecutableDependencyClass.class);
        RemoteExecService remoteExecService = client.getRemoteExecService();
        remoteExecService.execRemotely("localhost", TEST_PORT, config);

        Assert.assertTrue(testFile.exists());
        Assert.assertEquals("hi", FileUtils.readFileToString(testFile, StandardCharsets.UTF_8));

        serverContext.terminate();
        clientContext.terminate();
    }

    public static class Server {
        @Inject
        private RemoteExecRequestHandlerFactory remoteExecRequestHandlerFactory;
        @Inject
        private SocketServer socketServer;
    }

    public static class Client {
        @Inject
        private RemoteExecService remoteExecService;
        @Inject
        private ApplicationContext applicationContext;

        public RemoteExecService getRemoteExecService() {
            return remoteExecService;
        }
    }

}