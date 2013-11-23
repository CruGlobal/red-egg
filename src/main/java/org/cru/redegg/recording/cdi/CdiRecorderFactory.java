package org.cru.redegg.recording.cdi;

import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.recording.impl.DefaultErrorRecorder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Instance;
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

    @Override
    public ErrorRecorder getRecorder() {
        if (beanManager.getContext(RequestScoped.class).isActive())
            return webErrorRecorder;
        else
            return new DefaultErrorRecorder();
    }
}
