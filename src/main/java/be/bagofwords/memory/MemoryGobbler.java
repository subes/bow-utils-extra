package be.bagofwords.memory;

/**
 * Interface to be implemented by components that use up a lot of memory and that can free some memory if
 * the JVM is running low on memory (e.g. caches)
 */

public interface MemoryGobbler {

    long freeMemory();

    long getMemoryUsage();

}
