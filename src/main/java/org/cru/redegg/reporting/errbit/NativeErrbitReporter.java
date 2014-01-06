package org.cru.redegg.reporting.errbit;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import org.apache.log4j.Logger;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.api.ErrorReporter;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

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
        if (report.isUserError())
            logUserWarning(report);
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
            sendXmlReport(payload);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private int sendXmlReport(ErrbitXmlPayload payload) throws IOException
    {
        HttpURLConnection urlConnection = buildConnection();
        configure(urlConnection);
        sendXmlPayload(payload, urlConnection);
        int responseCode = urlConnection.getResponseCode();
        if (responseCode >= 400)
        {
            throw new RuntimeException(
                "notice not successfully submitted; response code: " + responseCode +
                "; content:\n" + getContent(urlConnection));
        }

        return responseCode;
    }

    private String getContent(HttpURLConnection urlConnection) throws IOException
    {
        InputStream errorStream = urlConnection.getErrorStream();
        if (errorStream == null)
            return "(no content)";
        else
        {
            return readErrorStream(errorStream);
        }
    }

    private String readErrorStream(InputStream errorStream) throws IOException
    {
        Reader reader = new InputStreamReader(errorStream, Charsets.UTF_8);
        try
        {
            return CharStreams.toString(reader);
        }
        finally
        {
            Closeables.closeQuietly(reader);
        }
    }

    private void sendXmlPayload(ErrbitXmlPayload payload, HttpURLConnection urlConnection) throws IOException
    {
        urlConnection.connect();
        OutputStream outputStream = urlConnection.getOutputStream();
        Writer writer = new OutputStreamWriter(outputStream, Charsets.UTF_8);
        try
        {
            payload.writeXmlTo(writer);
        }
        finally
        {
            Closeables.closeQuietly(writer);
        }
    }

    private void configure(HttpURLConnection urlConnection) throws ProtocolException
    {
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/xml");
        urlConnection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(10));
        urlConnection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(30));
    }

    private HttpURLConnection buildConnection() throws IOException
    {
        URI endpoint = config.getEndpoint();
        if (endpoint == null)
            throw new IllegalArgumentException("no endpoint is configured!");

        return (HttpURLConnection) endpoint.toURL().openConnection();
    }
}
