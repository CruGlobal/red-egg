package org.cru.redegg.recording.cdi;

import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.Serializer;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.recording.impl.DefaultErrorRecorder;
import org.cru.redegg.reporting.api.ErrorQueue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

/**
 * @author Matt Drees
 */
@ApplicationScoped
public class CdiRecorderFactory implements RecorderFactory {

    @Inject
    BeanManager beanManager;

    @Inject
    WebErrorRecorder webErrorRecorder;

    @Inject
    ErrorQueue errorQueue;

    @Inject
    Serializer serializer;

    @Override
    public ErrorRecorder getRecorder() {
        if (beanManager.getContext(RequestScoped.class).isActive())
            return webErrorRecorder;
        else
            return new DefaultErrorRecorder(errorQueue, serializer);
    }

    @Override
    public WebErrorRecorder getWebRecorder()
    {
        if (!beanManager.getContext(RequestScoped.class).isActive())
            throw new IllegalStateException("there is no web request being processed on this thread");
        return webErrorRecorder;
    }
}
