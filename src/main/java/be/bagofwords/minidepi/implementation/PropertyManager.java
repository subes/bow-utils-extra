/*******************************************************************************
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-11. For license
 * information see the LICENSE file in the root folder of this repository.
 ******************************************************************************/

package be.bagofwords.minidepi.implementation;

import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.PropertyException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class PropertyManager {

    private Properties properties;
    private Map<String, Properties> readPropertyResources = new HashMap<>();
    private List<File> readPropertyFiles = new ArrayList<>();

    public PropertyManager(Properties properties) {
        this.properties = properties;
        init();
    }

    public void init() {
        properties.putAll(System.getProperties());
        readPropertiesFromPropertyFiles(properties);
    }

    public void readPropertiesFromPropertyFiles(Properties properties) {
        String propertyFile = properties.getProperty("property.file");
        if (propertyFile != null) {
            readPropertiesFromFile(properties, propertyFile);
        }
        String propertyFiles = properties.getProperty("property.files");
        if (propertyFiles != null) {
            String[] propertyFilesArr = propertyFiles.split(",");
            for (String file : propertyFilesArr) {
                readPropertiesFromFile(properties, file);
            }
        }
    }

    public void readPropertiesFromFile(Properties properties, String path) {
        path = path.trim();
        File propertiesFile = new File(path);
        if (!propertiesFile.exists()) {
            throw new RuntimeException("Could not find file " + propertiesFile.getAbsolutePath());
        }
        try {
            Properties newProperties = new Properties();
            newProperties.load(new FileInputStream(propertiesFile));
            readPropertiesFromPropertyFiles(newProperties);
            properties.putAll(newProperties);
            readPropertyFiles.add(propertiesFile);
        } catch (IOException e) {
            throw new PropertyException("Failed to read properties from file " + propertiesFile.getAbsolutePath(), e);
        }
        Log.i("Read properties from " + path);
    }


    public String getProperty(String name, String orFrom) {
        String value = properties.getProperty(name);
        if (value == null) {
            return readPropertyFromFallbackResource(name, orFrom); //Should always return a value, since a fallback file was specified
        } else {
            return value;
        }
    }

    public String getProperty(String name) {
        String value = properties.getProperty(name);
        if (value == null) {
            printInformationOnPropertyFiles(name);
            throw new PropertyException("The property " + name + " was not found. See the logs on how to resolve this");
        }
        return value;
    }

    public void printInformationOnPropertyFiles(String propertyName) {
        Log.e("Property \"" + propertyName + "\" was not found! This will terminate your application.");
        if (readPropertyFiles.isEmpty()) {
            Log.e("You can provide a property file by passing setting a system property, i.e. -Dproperty.file=/path/to/my_properties.properties");
        } else if (readPropertyFiles.size() == 1) {
            Log.e("You might want to add the property to the file " + readPropertyFiles.get(0).getAbsolutePath() + " to avoid this error on the next run");
        } else {
            readPropertyFiles.size();
            Log.e("You might want to add the property any of the following files to avoid this error on the next run");
            for (File file : readPropertyFiles) {
                Log.e("\t" + file.getAbsolutePath());
            }
        }
    }

    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

    private synchronized String readPropertyFromFallbackResource(String name, String propertyResource) {
        if (!readPropertyResources.containsKey(propertyResource)) {
            InputStream defaultPropertiesInputStream = this.getClass().getResourceAsStream("/" + propertyResource);
            if (defaultPropertiesInputStream == null) {
                throw new PropertyException("Could not read resource /" + propertyResource);
            } else {
                try {
                    Properties properties = new Properties();
                    properties.load(defaultPropertiesInputStream);
                    readPropertyResources.put(propertyResource, properties);
                    Log.i("Read properties from resource " + propertyResource);
                } catch (IOException e) {
                    throw new PropertyException("Could not load properties from resource /" + propertyResource, e);
                }
            }
        }
        Properties properties = readPropertyResources.get(propertyResource);
        String value = properties.getProperty(name);
        if (value == null) {
            //Should not happen normally, since the developer of the specific library specified the propertyResource
            throw new PropertyException("The configuration option " + name + " was not found in default properties " + propertyResource);
        }
        return value;
    }
}
