/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2018-12-6. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.jobs;

public interface Job<T extends Object> {

    void doAction(T target) throws Exception;

}