package org.cru.redegg.reporting.errbit;

import airbrake.AirbrakeNotice;
import airbrake.AirbrakeNotifier;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.api.ErrorReporter;
import org.cru.redegg.reporting.errbit.ErrbitConfig;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

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

    AirbrakeNotifier notifier = new AirbrakeNotifier(config.getEndpoint().toString());

    public void send(ErrorReport report)
    {
        AirbrakeNotice notice = RedEggAirbreakNoticeBuilder.build(config, report).newNotice();
        int responseStatus = notifier.notify(notice);
        if (responseStatus != 200)
            throw new RuntimeException("notice not successfully submitted; response status: " + responseStatus);
    }
}
