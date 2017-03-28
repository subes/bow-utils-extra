/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.util;

import be.bagofwords.ui.UI;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ConcurrencyUtils {

    public static void terminateAndWaitForExecutorService(ExecutorService executorService, String name, int numberOfSeconds) {
        executorService.shutdown();
        try {
            executorService.awaitTermination(numberOfSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //OK, we were cleaning up anyway
        }
        if (!executorService.isTerminated()) {
            UI.write("The handler threads for " + name + " did not terminate in 10s");
        }
    }

    public static ExecutorService multiThreadedExecutor(String name) {
        return new CleanForkJoinPool(Runtime.getRuntime().availableProcessors(), pool -> new CleanForkJoinWorkerThread(name, pool), null, true, name);
    }

}
