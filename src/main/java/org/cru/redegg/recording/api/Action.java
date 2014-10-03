package org.cru.redegg.recording.api;

import javax.interceptor.InterceptorBinding;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * Indicates that the annotated method is the 'action' for this web request.
 * See {@link WebErrorRecorder#recordComponent(Method)}.
 *
 * If this annotates a class, then all of the public methods are 'action' methods.
 *
 * Note: requires CDI in order to be effective.
 * Also, the app's beans.xml needs to activate the corresponding interceptor:
 * <pre> {@code <interceptors>
 *     <class>org.cru.redegg.recording.interceptor.ActionRecordingInterceptor</class>
 * </interceptor>}</pre>
 *
 *
 * @author Matt Drees
 */
@Documented
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Action
{
}
