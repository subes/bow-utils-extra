/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-11-25. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.util;

import be.bagofwords.logging.Log;
import be.bagofwords.web.BaseController;

import java.util.Arrays;

import static java.util.stream.Collectors.toList;

public class MicroAnalytics {

    public static void track(Object... args) {
        String message = String.join(" ", Arrays.stream(args).map(arg -> convertArgToString(arg)).collect(toList()));
        Log.i("ANALYTICS " + message);
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
