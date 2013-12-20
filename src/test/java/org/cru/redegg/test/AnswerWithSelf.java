package org.cru.redegg.test;

import org.mockito.internal.stubbing.defaultanswers.ReturnsEmptyValues;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * from http://jakegoulding.com/blog/2012/01/09/stubbing-builder-pattern-in-mockito/
 * Returns the mock object itself for any method that returns the specified class.
 */
public class AnswerWithSelf implements Answer<Object>
{
    private final Answer<Object> delegate = new ReturnsEmptyValues();
    private final Class<?> clazz;

    public AnswerWithSelf(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Object answer(InvocationOnMock invocation) throws Throwable {
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (returnType.isAssignableFrom(clazz)) {
            return invocation.getMock();
        } else {
            return delegate.answer(invocation);
        }
    }
}
