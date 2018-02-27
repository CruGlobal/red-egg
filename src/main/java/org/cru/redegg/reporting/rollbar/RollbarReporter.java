package org.cru.redegg.reporting.rollbar;

import com.google.common.base.Throwables;
import com.rollbar.sender.PayloadSender;
import com.rollbar.sender.RollbarResponse;
import org.cru.redegg.reporting.api.ErrorLink;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.api.ErrorReporter;

import java.net.MalformedURLException;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Matt Drees
 */
public class RollbarReporter implements ErrorReporter
{


    private final RollbarConfig config;
    private final PayloadSender sender;

    public RollbarReporter(RollbarConfig config)
    {
        this.config = config;
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

    @Override
    public Optional<ErrorLink> buildLink()
    {
        return Optional.of(new RollbarErrorLink(UUID.randomUUID()));
    }
}
