package org.cru.redegg.reporting.errbit;

import org.apache.log4j.Logger;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.api.ErrorReporter;
import org.cru.redegg.reporting.common.HttpPayloadSender;

import javax.inject.Inject;
import java.io.IOException;

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

    Logger log = Logger.getLogger(getClass());

    ErrbitConfig config;

    @Inject
    public NativeErrbitReporter(ErrbitConfig config)
    {
        this.config = config;
    }

    public void send(ErrorReport report)
    {
        config.validate();
        if (report.isUserError())
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
        log.info("user error: " + report.getRootErrorMessage().or("<message not available>"));
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
