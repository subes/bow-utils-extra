/*******************************************************************************
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-1. For license
 * information see the LICENSE file in the root folder of this repository.
 ******************************************************************************/

package be.bagofwords.minidepi;

public class PropertyException extends ApplicationContextException {
    public PropertyException(String message) {
        super(message);
    }

    public PropertyException(String message, Throwable cause) {
        super(message, cause);
    }
}
