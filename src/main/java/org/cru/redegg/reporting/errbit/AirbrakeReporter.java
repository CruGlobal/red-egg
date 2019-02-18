package org.cru.redegg.reporting.errbit;

import airbrake.AirbrakeNotice;
import airbrake.AirbrakeNotifier;
import org.cru.redegg.reporting.api.ErrorLink;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.api.ErrorReporter;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.net.URI;
import java.util.Optional;

/**
 * A reporter that uses Airbrake's java client:
 * https://github.com/airbrake/airbrake-java
 *
 * To use this instead of NativeErrbitReporter, add to your alternatives list in beans.xml:
 *
 * <pre>{@code
 * <alternatives>
 *     <class>org.cru.redegg.reporting.errbit.AirbrakeReporter</class>
 * </alternatives>
 * }</pre>
 *
 * Note: this hasn't been tested with the real Airbrake service, or with Errbit for that matter.
 * It was my original plan to use this for reporting errors to Errbit,
 * but I eventually wrote NativeErrbitReporter
 * to take advantage of some of the features of errbit (user context, github linking).
 *
 * @author Matt Drees
 */
@Alternative
public class AirbrakeReporter implements ErrorReporter
{

    @Inject
    ErrbitConfig config;

    AirbrakeNotifier notifier;

    @PostConstruct
    public void init()
    {
        URI endpoint = config.getEndpoint();
        if (endpoint != null)
        {
            notifier = new AirbrakeNotifier(endpoint.toString());
        }
        else
        {
            notifier = new AirbrakeNotifier();
        }
    }

    public void send(ErrorReport report)
    {
        AirbrakeNotice notice = RedEggAirbreakNoticeBuilder.build(config, report).newNotice();
        int responseStatus = notifier.notify(notice);
        if (responseStatus != 200)
            throw new RuntimeException("notice not successfully submitted; response status: " + responseStatus);
    }

    @Override
    public Optional<ErrorLink> buildLink()
    {
        return Optional.empty();
    }
}
