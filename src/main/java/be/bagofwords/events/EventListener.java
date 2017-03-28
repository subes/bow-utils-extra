package be.bagofwords.events;

/**
 * Created by koen on 4/03/17.
 */
public interface EventListener<T> {

    void handleEvent(T data);

}
