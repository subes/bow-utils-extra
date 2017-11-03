/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-8-15. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagwords.exec;

import be.bagofwords.exec.PackedRemoteObject;
import be.bagofwords.exec.RemoteObjectConfig;
import be.bagofwords.exec.RemoteObjectUtil;
import be.bagofwords.logging.Log;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

public class RemoteObjectTest {

    @Test
    public void testSeparateClass() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        System.out.println("Dummy runnable class " + DummyRunnable.class);
        Log.i("Hello from test");
        File testFile = new File(DummyRunnable.testFile);
        String message = "hello there";
        Assert.assertTrue(!testFile.exists() || testFile.delete());
        PackedRemoteObject packedRemoteObject = RemoteObjectConfig.create(new DummyRunnable(message)).add(DependencyClass.class).pack();
        Object runner = RemoteObjectUtil.loadObject(packedRemoteObject);
        runner.getClass().getMethod("run").invoke(runner);
        Assert.assertTrue(testFile.exists());
        Assert.assertEquals(message, FileUtils.readFileToString(testFile, StandardCharsets.UTF_8));
    }

    @Test(expected = RuntimeException.class)
    public void testInnerClass() {
        PackedRemoteObject packedRemoteObject = RemoteObjectConfig.create(new InnerDummyRunnable()).add(DependencyClass.class).pack();
        DummyRunnable runner = (DummyRunnable) RemoteObjectUtil.loadObject(packedRemoteObject);
        runner.run();
    }

    public static class InnerDummyRunnable implements Runnable, Serializable {

        @Override
        public void run() {
            DependencyClass dependency = new DependencyClass();
            dependency.doSomething();
        }
    }

}