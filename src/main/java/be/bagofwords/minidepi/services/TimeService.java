/*******************************************************************************
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-6-21. For license
 * information see the LICENSE file in the root folder of this repository.
 ******************************************************************************/

package be.bagofwords.minidepi.services;

import java.util.Date;

public class TimeService {

    public void sleep(long timeToSleep) throws InterruptedException {
        Thread.sleep(timeToSleep);
    }

    public void sleepNoInterrupt(long timeToSleep) {
        try {
            sleep(timeToSleep);
        } catch (InterruptedException e) {
            throw new RuntimeException("Got interrupt while sleeping for " + timeToSleep + " ms", e);
        }
    }

    public long getTime() {
        return getNow().getTime();
    }

    public Date getNow() {
        return new Date();
    }

}
