/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-11-25. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.util;

import be.bagofwords.logging.Log;
import be.bagofwords.web.BaseController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

public class MicroAnalytics {

    private static final List<Consumer<String>> listeners = new ArrayList<>();

    public static void track(Object... args) {
        String message = String.join(" ", Arrays.stream(args).map(arg -> convertArgToString(arg)).collect(toList()));
        synchronized (listeners) {
            Iterator<Consumer<String>> iterator = listeners.iterator();
            while (iterator.hasNext()) {
                try {
                    iterator.next().accept(message);
                } catch (Exception exp) {
                    Log.w("Analytics listener threw an exception, will remove this listener", exp);
                    iterator.remove();
                }
            }
        }
        Log.i("ANALYTICS " + message);
    }

    public static void addAnalyticsListener(Consumer<String> listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    private static String convertArgToString(Object arg) {
        if (arg instanceof String) {
            return (String) arg;
        } else if (arg instanceof BaseController) {
            BaseController controller = (BaseController) arg;
            return controller.getMethod() + " " + controller.getPath();
        } else {
            return SerializationUtils.serializeObject(arg);
        }
    }

}
