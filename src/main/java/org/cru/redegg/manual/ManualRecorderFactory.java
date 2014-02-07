package org.cru.redegg.manual;

import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.WebErrorRecorder;

/**
 * @author Matt Drees
 */
public class ManualRecorderFactory implements RecorderFactory
{

    private final Builder builder;

    public ManualRecorderFactory(Builder builder)
    {
        this.builder = builder;
    }

    @Override
    public ErrorRecorder getRecorder()
    {
        if (WebRequest.isActive())
            return getWebRecorder();
        else
            return builder.buildDefaultErrorRecorder();
    }

    @Override
    public WebErrorRecorder getWebRecorder()
    {
        return WebRequest.get().getRecorder();
    }
}
