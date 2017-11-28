/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-11-25. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.util;

import be.bagofwords.logging.Log;
import be.bagofwords.web.BaseController;

import java.util.*;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

public class MicroAnalytics {

    private static final List<Consumer<AnalyticsMessage>> listeners = new ArrayList<>();

    public static void track(Object... args) {
        Date sendDate = new Date();
        String message = String.join(" ", Arrays.stream(args).map(arg -> convertArgToString(arg)).collect(toList()));
        synchronized (listeners) {
            Iterator<Consumer<AnalyticsMessage>> iterator = listeners.iterator();
            while (iterator.hasNext()) {
                try {
                    iterator.next().accept(new AnalyticsMessage(message, sendDate));
                } catch (Exception exp) {
                    Log.w("Analytics listener threw an exception, will remove this listener", exp);
                    iterator.remove();
                }
            }
        }
        Log.i("ANALYTICS " + message);
    }

    public static void addAnalyticsListener(Consumer<AnalyticsMessage> listener) {
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

    public static class AnalyticsMessage {
        public String message;
        public Date timestamp;

        public AnalyticsMessage(String message, Date timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }

    }

}
