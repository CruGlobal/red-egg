package org.cru.redegg.manual;

import org.cru.redegg.boot.Lifecycle;
import org.cru.redegg.recording.api.RecorderFactory;

/**
 * @author Matt Drees
 */
public class ManualLifecycle extends Lifecycle
{
    private final Builder builder;

    public ManualLifecycle(RecorderFactory recorderFactory, Builder builder)
    {
        super(recorderFactory);
        this.builder = builder;
    }

    @Override
    public void beginApplication()
    {
        super.beginApplication();
    }

    @Override
    public void endApplication()
    {
        super.endApplication();
        builder.shutdown();
    }

    @Override
    public void beginRequest()
    {
        super.beginRequest();
        WebRequest.begin(builder);
    }

    @Override
    public void endRequest()
    {
        super.endRequest();
        WebRequest.end();
    }
}
