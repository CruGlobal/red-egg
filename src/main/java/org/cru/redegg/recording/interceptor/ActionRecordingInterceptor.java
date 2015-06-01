package org.cru.redegg.recording.interceptor;

import org.cru.redegg.recording.api.Action;
import org.cru.redegg.recording.api.WebErrorRecorder;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * @author Matt Drees
 */
@Action
@Interceptor
public class ActionRecordingInterceptor
{
    @Inject
    WebErrorRecorder recorder;

    @AroundInvoke
    public Object recordAction(InvocationContext context) throws Exception
    {
        recorder.recordComponent(context.getMethod());
        return context.proceed();
    }
}
