/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-17. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.application;

import be.bagofwords.cache.CachesManager;
import be.bagofwords.memory.MemoryManager;
import be.bagofwords.minidepi.ApplicationContext;

public class MinimalApplicationContextFactory {

    public void wireApplicationContext(ApplicationContext context) {
        MemoryManager memoryManager = new MemoryManager();
        BowTaskScheduler bowTaskScheduler = new BowTaskScheduler();
        context.declareBean(memoryManager);
        context.declareBean(bowTaskScheduler);
        CachesManager cachesManager = new CachesManager(context);
        context.declareBean(cachesManager);
    }
}
