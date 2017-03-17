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
