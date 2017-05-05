/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-5-5. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.events;

import be.bagofwords.exec.RemoteClass;
import be.bagofwords.minidepi.events.EventFilter;

@RemoteClass
public class TestEventFilter implements EventFilter<RemoteEventServiceTest.Event> {

    @Override
    public boolean accept(RemoteEventServiceTest.Event event) {
        return true;
    }
}
