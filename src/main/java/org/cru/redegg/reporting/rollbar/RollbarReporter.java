package org.cru.redegg.reporting.rollbar;

import com.google.common.base.Throwables;
import com.rollbar.sender.PayloadSender;
import com.rollbar.sender.RollbarResponse;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.api.ErrorReporter;

import javax.inject.Inject;
import java.net.MalformedURLException;

/**
 * @author Matt Drees
 */
public class RollbarReporter implements ErrorReporter
{


    private final RollbarConfig config;
    private final FileNameResolver resolver;
    private final PayloadSender sender;

    @Inject
    public RollbarReporter(RollbarConfig config, FileNameResolver resolver)
    {
        this.config = config;
        this.resolver = resolver;
        try
        {
            this.sender = new PayloadSender(config.getEndpoint().toURL());
        }
        catch (MalformedURLException e)
        {
            throw Throwables.propagate(e);
        }
    }


    @Override
    public void send(ErrorReport report)
    {
        RollbarPayloadBuilder builder = new RollbarPayloadBuilder(config, report);

        RollbarResponse response = sender.send(builder.build());
        if (!response.isSuccessful())
        {
            throw new RuntimeException("unsuccessful attempt to send rollbar report: " + response.errorMessage());
        }
    }
}
