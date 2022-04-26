/*******************************************************************************
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-17. For license
 * information see the LICENSE file in the root folder of this repository.
 ******************************************************************************/

package be.bagofwords.minidepi.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({CONSTRUCTOR, FIELD})
@Retention(RUNTIME)
public @interface Property {
    String value();

    String orFrom() default "";
}
