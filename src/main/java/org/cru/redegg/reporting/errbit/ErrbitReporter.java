package org.cru.redegg.reporting.errbit;

import airbrake.AirbrakeNotice;
import airbrake.AirbrakeNotifier;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.api.ErrorReporter;
import org.cru.redegg.reporting.errbit.ErrbitConfig;

import javax.inject.Inject;

/**
 * @author Matt Drees
 */
public class ErrbitReporter implements ErrorReporter
{

    @Inject
    ErrbitConfig config;

    AirbrakeNotifier notifier = new AirbrakeNotifier(config.getEndpoint().toString());

    public void send(ErrorReport report)
    {
        AirbrakeNotice notice = RedEggAirbreakNoticeBuilder.build(config, report).newNotice();
        int responseStatus = notifier.notify(notice);
        if (responseStatus != 200)
            throw new RuntimeException("notice not successfully submitted; response status: " + responseStatus);
    }
}
