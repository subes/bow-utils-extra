package be.bagofwords.jobs;

public interface PartitionableJob<T extends Object> {

    void doAction(int partition, T target) throws Exception     ;

}