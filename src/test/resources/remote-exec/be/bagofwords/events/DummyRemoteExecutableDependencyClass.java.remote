/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-7-29. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.events;

import be.bagofwords.exec.RemoteClass;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

@RemoteClass
public class DummyRemoteExecutableDependencyClass implements Serializable {

    public void doSomething() throws IOException {
        File file = new File(TestRemoteExecService.TEST_FILE);
        FileUtils.writeStringToFile(file, "hi", StandardCharsets.UTF_8);
    }

}
