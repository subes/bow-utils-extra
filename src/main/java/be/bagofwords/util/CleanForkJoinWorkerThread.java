/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.util;

import be.bagofwords.logging.Log;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

public class CleanForkJoinWorkerThread extends ForkJoinWorkerThread {

    public CleanForkJoinWorkerThread(String name, ForkJoinPool pool) {
        super(pool);
        setName("worker-thread-" + name);
    }

    @Override
    protected void onTermination(Throwable exception) {
        if (exception != null) {
            Log.e("Unexpected exception in " + getName(), exception);
        }
        super.onTermination(exception);
    }


}
