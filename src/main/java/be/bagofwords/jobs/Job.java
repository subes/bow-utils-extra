package be.bagofwords.jobs;

public interface Job<T extends Object> {

    void doAction(T target) throws Exception;

}