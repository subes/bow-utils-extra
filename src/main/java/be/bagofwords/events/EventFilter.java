/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-4-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.events;

import java.io.Serializable;

public interface EventFilter<T> extends Serializable {

    boolean accept(T event);

}
