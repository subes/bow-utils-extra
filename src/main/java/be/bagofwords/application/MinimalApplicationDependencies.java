/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-17. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.application;

import be.bagofwords.cache.CachesManager;
import be.bagofwords.jobs.AsyncJobService;
import be.bagofwords.memory.MemoryManager;
import be.bagofwords.minidepi.annotations.Inject;

public class MinimalApplicationDependencies {

    @Inject
    private MemoryManager memoryManager;
    @Inject
    private AsyncJobService asyncJobService;
    @Inject
    private CachesManager cachesManager;

}
