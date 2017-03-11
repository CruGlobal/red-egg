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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Matt Drees
 */
public class ExceptionDetailsExtractor
{

    public List<String> extractDetails(Throwable throwable)
    {
        List<PropertyDescriptor> descriptors =
            getNonstandardPropertyDescriptors(throwable);
        if (descriptors.isEmpty())
        {
            return Collections.emptyList();
        }
        else
        {
            return getDetails(throwable, descriptors);
        }
    }


    private List<String> getDetails(
        Throwable throwable,
        List<PropertyDescriptor> propertyDescriptors)
    {
        List<String> details = Lists.newArrayList();
        String summary = RedEggStrings.truncate(throwable.toString(), 100, "...");
        details.add(summary);
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors)
        {
            details.add(
                "  " +
                propertyDescriptor.getDisplayName() +
                ": " +
                readProperty(throwable, propertyDescriptor));
        }
        return details;
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
