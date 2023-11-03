package org.cru.redegg.reporting.rollbar;

import com.google.common.base.Throwables;
import org.cru.redegg.reporting.api.ErrorLink;
import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.Sender;
import com.rollbar.notifier.sender.SyncSender;
import com.rollbar.notifier.sender.exception.SenderException;
import com.rollbar.notifier.sender.listener.SenderListener;
import com.rollbar.notifier.sender.result.Response;
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
    private final Sender sender;

    public RollbarReporter(RollbarConfig config)
    {
        this.config = config;
        try
        {
            this.sender = new SyncSender.Builder()
                .url(config.getEndpoint().toURL())
                .build();
            sender.addListener(new Listener());
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
        sender.send(builder.build());
    }

    private class Listener implements SenderListener
    {
        @Override
        public void onResponse(Payload payload, Response response) { }

        @Override
        public void onError(Payload payload, Exception e)
        {
            if (e instanceof SenderException && e.getCause() instanceof RuntimeException)
            {
                throw (RuntimeException) e.getCause();
            }
            Throwables.propagateIfPossible(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<ErrorLink> buildLink()
    {
        return Optional.of(new RollbarErrorLink(UUID.randomUUID()));
    }
}
