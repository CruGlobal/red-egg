package org.cru.redegg.reporting.common;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author Matt Drees
 */
public class Reporters
{
    public static String buildSimplifiedMethodName(Method method)
    {
        return method.getName() + "(" + simpleParamList(method) + ")";
    }

    private static String simpleParamList(Method method)
    {
        Class<?>[] parameterTypes = method.getParameterTypes();
        List<String> parameterTypeNames = Lists.newArrayListWithCapacity(parameterTypes.length);
        for (Class<?> aClass : parameterTypes)
        {
            parameterTypeNames.add(aClass.getSimpleName());
        }
        return Joiner.on(',').join(parameterTypeNames);
    }

}
