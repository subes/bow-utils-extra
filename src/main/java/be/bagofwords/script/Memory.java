/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-7-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Memory {

    private Map<String, Object> objects = new HashMap<>();

    public Object get(String name) {
        return objects.get(name);
    }

    public void put(String name, Object object) {
        objects.put(name, object);
    }

    public List<Object> getAllObjects() {
        return new ArrayList<>(objects.values());
    }

    public void clean() {
        objects.clear();
    }

}
