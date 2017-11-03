/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-8-15. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagwords.exec;

import be.bagofwords.exec.RemoteClass;
import be.bagofwords.logging.Log;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

@RemoteClass
public class DummyRunnable implements Runnable, Serializable {

    public static String testFile = "/tmp/dummyRunnableTestFile.txt";

    private final String message;
    private final Object[] someClasses = new Object[0]; //Added this because array gave problems at some point

    public DummyRunnable(String message) {
        this.message = message;
    }

    @Override
    public void run() {
        DependencyClass dependency = new DependencyClass();
        dependency.doSomething();
        try {
            System.out.println("Dummy runnable class " + DummyRunnable.class);
            Log.i("Hi there from dummy runnable!");
            FileUtils.write(new File(testFile), message, "UTF-8");
        } catch (IOException exp) {
            throw new RuntimeException("Failed to write to test file", exp);
        }
    }
}
