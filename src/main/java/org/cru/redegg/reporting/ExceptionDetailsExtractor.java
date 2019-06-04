package org.cru.redegg.reporting;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import org.cru.redegg.util.RedEggStrings;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Matt Drees
 */
public class ExceptionDetailsExtractor
{

    public Map<String, Object> extractDetails(Throwable throwable)
    {
        List<PropertyDescriptor> descriptors =
            getNonstandardPropertyDescriptors(throwable);
        if (descriptors.isEmpty())
        {
            return Collections.emptyMap();
        }
        else
        {
            return getDetails(throwable, descriptors);
        }
    }


    private Map<String, Object> getDetails(
        Throwable throwable,
        List<PropertyDescriptor> propertyDescriptors)
    {
        Map<String, Object> details = new HashMap<>();
        String summary = RedEggStrings.truncate(throwable.toString(), 100, "...");
        details.put("summary", summary);
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors)
        {
            details.put(
                propertyDescriptor.getDisplayName(),
                toPseudoJson(readProperty(throwable, propertyDescriptor)));
        }
        return details;
    }

    /**
     * Returns a structure of objects understood by {@link com.rollbar.utilities.RollbarSerializer}.
     */
    private Object toPseudoJson(Object value)
    {
        if (value == null) {
            return null;
        }
        else if (value instanceof Boolean) {
            return value;
        }
        else if (value instanceof Number) {
            return value;
        }
        else if (value instanceof String) {
            return value;
        }
        else if (value instanceof Map) {
            return mapToPseudoJson((Map) value);
        }
        else if (value instanceof Collection) {
            return collectionToPseudoJson((Collection) value);
        }
        else if (value instanceof Object[]) {
            return collectionToPseudoJson(Arrays.asList((Object[]) value));
        }
        else {
            return value.toString();
        }
    }

    private Object collectionToPseudoJson(Collection<?> input)
    {
        return input.stream()
            .map(this::toPseudoJson)
            .collect(Collectors.toList());
    }

    private Map<String, Object> mapToPseudoJson(Map<?, ?> input) {
        return input.entrySet()
            .stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().toString(),
                entry -> toPseudoJson(entry.getValue())
            ));
    }


    /** gets properties that are not boring ones, like getClass(), getMessage(), getCause(), etc */
    private List<PropertyDescriptor> getNonstandardPropertyDescriptors(Throwable throwable)
    {
        Class<? extends Throwable> aClass = throwable.getClass();
        List<PropertyDescriptor> descriptors = Lists.newArrayList(getPropertyDescriptors(aClass));

        List<PropertyDescriptor> standardPropertyDescriptors =
            Arrays.asList(getPropertyDescriptors(Throwable.class));

        descriptors.removeAll(standardPropertyDescriptors);

        return descriptors;
    }

    private PropertyDescriptor[] getPropertyDescriptors(Class<? extends Throwable> aClass)
    {
        BeanInfo info;
        try
        {
            info = Introspector.getBeanInfo(aClass);
        }
        catch (IntrospectionException e)
        {
            throw Throwables.propagate(e);
        }
        return info.getPropertyDescriptors();
    }

    private Object readProperty(Throwable throwable, PropertyDescriptor propertyDescriptor)
    {
        Method readMethod = propertyDescriptor.getReadMethod();
        if (readMethod == null)
            return "<cannot be read>";
        try
        {
            return readMethod.invoke(throwable);
        }
        catch (
                // PropertyDescriptors should not hand out read methods that aren't public,
                // so IllegalAccessException should never happen.
                // But since this is error-handling code,
                // I'll be a little more conservative and not throw an AssertionError.
                IllegalAccessException |

                InvocationTargetException e)
        {
            return "<unavailable: " + e + " >";
        }
    }

}
