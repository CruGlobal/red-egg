package org.cru.redegg.reporting.errbit;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
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
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * @author Matt Drees
 *
 * Reports errors to an Errbit instance using the v2.4 xml api.
 */
public class NativeErrbitReporter implements ErrorReporter
{

    ErrbitConfig config;

    @Inject
    public NativeErrbitReporter(ErrbitConfig config)
    {
        this.config = config;
    }

    public void send(ErrorReport report)
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
        if (responseCode != 200)
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
        URL endpoint = config.getEndpoint();
        if (endpoint == null)
            throw new IllegalArgumentException("no endpoint is configured!");

        return (HttpURLConnection) endpoint.openConnection();
    }
}
