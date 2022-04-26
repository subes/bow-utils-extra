/*******************************************************************************
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-11. For license
 * information see the LICENSE file in the root folder of this repository.
 ******************************************************************************/

package be.bagofwords.minidepi;

import be.bagofwords.logging.Log;

import java.util.Collections;
import java.util.Map;

public class ApplicationManager {

    public static void run(Runnable main) {
        run(main, Collections.<String, String>emptyMap());
    }

    public static void run(Runnable main, Map<String, String> config) {
        run(main, new ApplicationContext(config));
    }

    public static void run(Runnable main, ApplicationContext applicationContext) {
        runImpl(main, applicationContext, true);
    }

    public static void run(Class<? extends Runnable> mainClass) {
        run(mainClass, Collections.<String, String>emptyMap());
    }

    public static void run(Class<? extends Runnable> mainClass, Map<String, String> config) {
        run(mainClass, new ApplicationContext(config));
    }

    public static void run(Class<? extends Runnable> mainClass, ApplicationContext applicationContext) {
        Runnable runnable = applicationContext.getBean(mainClass);
        runImpl(runnable, applicationContext, false);
    }

    private static void runImpl(Runnable main, ApplicationContext applicationContext, boolean wireRunnable) {
        try {
            if (wireRunnable) {
                applicationContext.registerBean(main);
            }
            main.run();
        } catch (Throwable exp) {
            Log.e("Received unexpected exception, terminating application.", exp);
        } finally {
            if (!applicationContext.terminateWasRequested()) {
                applicationContext.terminate();
            }
        }
    }

}
