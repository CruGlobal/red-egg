package org.cru.redegg.reporting.errbit;

import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.api.ErrorReporter;
import org.cru.redegg.reporting.common.HttpPayloadSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.cru.redegg.recording.api.NotificationLevel.WARNING;

/**
 * Reports errors to an Errbit instance using the v2.4 xml api.
 *
 * Errbit doesn't really distinguish user/client errors from server errors, so user errors are not reported.
 * Instead, we just log (info) a short summary message.
 *
 * @author Matt Drees
 */
public class NativeErrbitReporter implements ErrorReporter
{

    Logger log = LoggerFactory.getLogger(getClass());

    ErrbitConfig config;

    public NativeErrbitReporter(ErrbitConfig config)
    {
        this.config = config;
    }

    public void send(ErrorReport report)
    {
        config.validate();
        if (report.getNotificationLevel() == WARNING)
        {
            logUserWarning(report);
            if (report.isMustNotify())
                doSend(report);
        }
        else
            doSend(report);
    }

    private void logUserWarning(ErrorReport report)
    {
        log.info("user error: {}", report.getRootErrorMessage().or("<message not available>"));
    }


    private void doSend(ErrorReport report)
    {
        ErrbitXmlPayload payload = new ErrbitXmlPayload(report, config);
        try
        {
            new HttpPayloadSender(config.getEndpoint(), "application/xml").send(payload);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

}
