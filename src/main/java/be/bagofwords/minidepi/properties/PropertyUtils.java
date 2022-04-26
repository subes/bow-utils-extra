/*******************************************************************************
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2018-11-28. For license
 * information see the LICENSE file in the root folder of this repository.
 ******************************************************************************/

package be.bagofwords.minidepi.properties;

import java.util.Map;
import java.util.Properties;

public class PropertyUtils {

    public static Properties mapToProperties(Map<String, String> propertiesAsMap) {
        Properties properties = new Properties();
        properties.putAll(propertiesAsMap);
        return properties;
    }
}
