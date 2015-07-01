package org.cru.redegg.reporting.rollbar;

import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.api.ErrorReporter;
import org.cru.redegg.reporting.common.HttpPayloadSender;

import javax.inject.Inject;
import java.io.IOException;

/**
 * @author Matt Drees
 */
public class RollbarReporter implements ErrorReporter
{


    private final RollbarConfig config;
    private final FileNameResolver resolver;

    @Inject
    public RollbarReporter(RollbarConfig config, FileNameResolver resolver)
    {
        this.config = config;
        this.resolver = resolver;
    }


    @Override
    public void send(ErrorReport report)
    {
        RollbarJsonPayload payload = new RollbarJsonPayload(report, config, resolver);

        try
        {
            new HttpPayloadSender(config.getEndpoint(), "application/json").send(payload);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

    }
}
