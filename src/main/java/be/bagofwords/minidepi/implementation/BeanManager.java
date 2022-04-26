/*******************************************************************************
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-11. For license
 * information see the LICENSE file in the root folder of this repository.
 ******************************************************************************/

package be.bagofwords.minidepi.implementation;

import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.*;
import be.bagofwords.minidepi.annotations.Bean;
import be.bagofwords.minidepi.annotations.Inject;
import be.bagofwords.minidepi.annotations.Property;
import be.bagofwords.util.SerializationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class BeanManager {

    private final ApplicationContext applicationContext;
    private final BeanManager parentBeanManager;
    private final LifeCycleManager lifeCycleManager;
    private final List<QualifiedBean> beans = new ArrayList<>();
    private final Set<Class> beansBeingCreated = new HashSet<>();

    public BeanManager(ApplicationContext applicationContext, LifeCycleManager lifeCycleManager, BeanManager parentBeanManager) {
        this.applicationContext = applicationContext;
        this.lifeCycleManager = lifeCycleManager;
        this.parentBeanManager = parentBeanManager;
        saveBean(applicationContext);
    }

    private <T> void saveBean(T bean, String... names) {
        Set<String> qualifiers = getQualifiers(bean);
        for (String name : names) {
            qualifiers.add(name);
        }
        beans.add(new QualifiedBean(qualifiers, bean));
    }

    private <T> Set<String> getQualifiers(T bean) {
        return getQualifiers(bean.getClass());
    }

    private <T> Set<String> getQualifiers(Class<T> beanType) {
        Annotation[] annotations = beanType.getAnnotations();
        return getQualifiers(annotations);
    }

    private <T> Set<String> getQualifiers(Annotation[] annotations) {
        Set<String> qualifiers = new HashSet<>();
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.equals(Bean.class)) {
                Bean beanAnnotation = (Bean) annotation;
                String[] names = beanAnnotation.value();
                for (String name : names) {
                    if (name.length() > 0) {
                        qualifiers.add(name);
                    }
                }
            } else if (annotationType.equals(Inject.class)) {
                Inject injectAnnotation = (Inject) annotation;
                String name = injectAnnotation.value();
                if (name.length() > 0) {
                    qualifiers.add(name);
                }
            }
        }
        return qualifiers;
    }

    public <T> List<T> getBeans(Class<T> beanType, String... names) {
        List<T> result = new ArrayList<>();
        for (QualifiedBean qualifiedBean : beans) {
            if (beanType.isAssignableFrom(qualifiedBean.bean.getClass())) {
                boolean matchesQualifiers;
                if (names.length == 0) {
                    matchesQualifiers = true;
                } else {
                    matchesQualifiers = false;
                    for (String name : names) {
                        matchesQualifiers |= qualifiedBean.qualifiers.contains(name);
                    }
                }
                if (matchesQualifiers) {
                    result.add(beanType.cast(qualifiedBean.bean));
                }
            }
        }
        return result;
    }

    public <T> T getBeanIfPresent(Class<T> beanType, String... names) {
        List<T> beans = getBeans(beanType, names);
        if (beans.size() == 1) {
            return beans.get(0);
        } else if (beans.size() == 0) {
            return null;
        } else {
            String errorMessage = "Found " + beans.size() + " beans of type " + beanType;
            errorMessage = appendNames(errorMessage, names);
            throw new ApplicationContextException(errorMessage);
        }
    }

    public <T> T getBean(Class<T> beanType, String... names) {
        return getBeanImpl(true, beanType, names);
    }

    private <T> T getBeanImpl(boolean createIfNecessary, Class<T> beanType, String... names) {
        List<T> matchingBeans = getBeans(beanType, names);
        if (matchingBeans.size() == 1) {
            return matchingBeans.get(0);
        } else if (matchingBeans.size() > 1) {
            String errorMessage = "Found multiple matching beans for type " + beanType.getSimpleName();
            errorMessage = appendNames(errorMessage, names);
            throw new ApplicationContextException(errorMessage);
        } else if (parentBeanManager != null) {
            T bean = parentBeanManager.getBeanImpl(false, beanType, names);
            if (bean != null) {
                return bean;
            }
        }
        if (createIfNecessary) {
            //Does this type have the correct qualifiers? If yes, we create it
            if (doQualifiersMatch(beanType, names)) {
                createBean(beanType);
                matchingBeans = getBeans(beanType, names);
                if (matchingBeans.size() == 1) {
                    return matchingBeans.get(0);
                }
            }
            String errorMessage;
            if (matchingBeans.size() > 0) {
                errorMessage = "Found multiple beans with type " + beanType;
                errorMessage = appendNames(errorMessage, names);
                errorMessage += " : ";
                for (int i = 0; i < matchingBeans.size(); i++) {
                    errorMessage += matchingBeans.get(i).getClass().getSimpleName();
                    if (i < matchingBeans.size() - 1) {
                        errorMessage += ", ";
                    }
                }
            } else {
                errorMessage = "Could not find any bean with type " + beanType;
                errorMessage = appendNames(errorMessage, names);
            }
            throw new ApplicationContextException(errorMessage);
        } else {
            return null;
        }
    }

    public <T> boolean doQualifiersMatch(Class<T> beanType, String[] names) {
        Set<String> qualifiers = getQualifiers(beanType);
        boolean qualifiersMatch = names.length == 0;
        for (String name : names) {
            qualifiersMatch |= qualifiers.contains(name);
        }
        return qualifiersMatch;
    }

    public String appendNames(String errorMessage, String[] names) {
        if (names.length > 0) {
            errorMessage += " and names " + Arrays.toString(names);
        }
        return errorMessage;
    }

    private <T> boolean canInstantiateBean(Class<T> beanType) {
        return !Modifier.isAbstract(beanType.getModifiers());
    }

    private <T> T createBean(Class<T> beanType, String... extraNames) {
        if (!canInstantiateBean(beanType)) {
            throw new ApplicationContextException("Can not instantiate bean of type " + beanType + ". Is this an abstract type?");
        }
        if (beanType.equals(ApplicationContext.class)) {
            throw new ApplicationContextException("Refusing to create ApplicationContext as bean");
        }
        if (beansBeingCreated.contains(beanType)) {
            throw new ApplicationContextException("The bean " + beanType + " is already being created. Possibly cycle?");
        }
        beansBeingCreated.add(beanType);
        try {
            T newBean = constructBean(beanType);
            registerBean(newBean, extraNames);
            Log.i("Created bean " + newBean);
            return newBean;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ApplicationContextException("Failed to create bean " + beanType, e);
        } finally {
            beansBeingCreated.remove(beanType);
        }
    }

    private <T> T constructBean(Class<T> beanClass) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        String beanName = beanClass.getCanonicalName();
        try {
            //Constructor with a single argument, the application context?
            try {
                return beanClass.getConstructor(ApplicationContext.class).newInstance(applicationContext);
            } catch (NoSuchMethodException exp) {
                //OK, continue
            }
            //Constructor with @Inject annotation?
            for (Constructor<?> constructor : beanClass.getConstructors()) {
                if (hasInjectAnnotation(constructor.getDeclaredAnnotations())) {
                    Class<?>[] parameterTypes = constructor.getParameterTypes();
                    Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
                    Object[] args = new Object[parameterTypes.length];
                    for (int i = 0; i < args.length; i++) {
                        Set<String> qualifiers = getQualifiers(parameterAnnotations[i]);
                        args[i] = getBean(parameterTypes[i], qualifiers.toArray(new String[qualifiers.size()]));
                    }
                    return (T) constructor.newInstance(args);
                }
            }
            //Constructor without any arguments?
            try {
                return beanClass.getConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                throw new ApplicationContextException("Could not create bean of type " + beanName + ". " + "Need at least one constructor that has either (1) no parameters, or (2) a single parameter of type ApplicationContext, or (3) the @Inject " +
                        "annotation");
            }
        } catch (Throwable exp) {
            if (exp instanceof ApplicationContextException) {
                throw exp;
            } else {
                throw new ApplicationContextException("Could not create bean of type " + beanName, exp);
            }
        }
    }

    public <T> void registerBean(Class<T> beanClass, String... names) {
        List<T> beans = getBeans(beanClass, names);
        if (beans.isEmpty()) {
            createBean(beanClass, names);
        }
    }

    public void registerBean(Object bean, String... names) {
        saveBean(bean, names);
        wireBean(bean);
        if (bean instanceof LifeCycleBean) {
            lifeCycleManager.ensureBeanCorrectState((LifeCycleBean) bean);
        }
    }

    public void wireBean(Object bean) {
        try {
            Class<?> currClass = bean.getClass();
            while (!currClass.equals(Object.class)) {
                wireFields(bean, currClass);
                if (bean instanceof OnWiredListener) {
                    ((OnWiredListener) bean).onWired(applicationContext);
                }
                currClass = currClass.getSuperclass();
            }
        } catch (IllegalAccessException exp) {
            throw new ApplicationContextException("Failed to wire bean " + bean, exp);
        }
    }


    private void wireFields(Object bean, Class<?> beanClass) throws IllegalAccessException {
        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            Inject injectAnnotation = field.getAnnotation(Inject.class);
            if (injectAnnotation != null) {
                Object value = injectDependentBean(bean, field);
                if (injectAnnotation.ensureStarted()) {
                    lifeCycleManager.registerStartBeforeDependency(bean, value);
                }
            } else {
                Property propertyAnnotation = field.getAnnotation(Property.class);
                if (propertyAnnotation != null) {
                    injectProperty(bean, field, propertyAnnotation);
                }
            }
        }
    }

    private void injectProperty(Object bean, Field field, Property propertyAnnotation) throws IllegalAccessException {
        String propertyName = propertyAnnotation.value();
        String value;
        try {
            if ("".equals(propertyAnnotation.orFrom())) {
                value = applicationContext.getProperty(propertyName);
            } else {
                value = applicationContext.getProperty(propertyName, propertyAnnotation.orFrom());
            }
        } catch (PropertyException exp) {
            throw new ApplicationContextException("Could not find property \"" + propertyName + "\" for bean " + bean, exp);
        }
        field.setAccessible(true);

        Object convertedValue;
        if (field.getType() != String.class) {
            convertedValue = convertValue(field, value, bean);
        } else {
            convertedValue = value;
        }
        field.set(bean, convertedValue);
    }

    private Object convertValue(Field field, String value, Object bean) {
        return SerializationUtils.deserializeObject(value, field.getType());
    }

    private Object injectDependentBean(Object bean, Field field) throws IllegalAccessException {
        field.setAccessible(true);
        if (field.get(bean) == null) {
            Class<?> fieldType = field.getType();
            Object newValue;
            if (fieldType == List.class) {
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    Type[] genericTypeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
                    if (genericTypeArgs.length == 1) {
                        newValue = getBeans((Class) genericTypeArgs[0]);
                    } else {
                        throw new ApplicationContextException("Received multiple types for List???");
                    }
                } else {
                    throw new ApplicationContextException("Could not determine generic type of field " + field.getName() + " in " + bean);
                }
            } else {
                Set<String> qualifiersSet = getQualifiers(field.getAnnotations());
                String[] qualifiers = qualifiersSet.toArray(new String[qualifiersSet.size()]);
                newValue = getBean(fieldType, qualifiers);
            }
            field.set(bean, newValue);
            return newValue;
        } else {
            throw new RuntimeException("The field " + field.getName() + " of " + bean + " was not null!");
        }
    }

    private boolean hasInjectAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(Inject.class)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasWiredFields(Object object) {
        Class<?> currClass = object.getClass();
        while (!currClass.equals(Object.class)) {
            Field[] fields = object.getClass().getDeclaredFields();
            for (Field field : fields) {
                Inject injectAnnotation = field.getAnnotation(Inject.class);
                if (injectAnnotation != null) {
                    return true;
                }
            }
            currClass = currClass.getSuperclass();
        }
        return false;
    }
}
