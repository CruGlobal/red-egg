package org.cru.redegg.reporting.common;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;

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
 * @author Matt Drees
 */
public class HttpPayloadSender
{

    private final URI endpoint;
    private final String contentType;

    public HttpPayloadSender(URI endpoint, String contentType)
    {
        this.endpoint = endpoint;
        this.contentType = contentType;
    }

    public int send(Payload payload) throws IOException
    {
        HttpURLConnection urlConnection = buildConnection();
        configure(urlConnection);
        sendPayload(payload, urlConnection);
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
        boolean threw = true;
        try
        {
            String content = CharStreams.toString(reader);
            threw = false;
            return content;
        }
        finally
        {
            Closeables.close(reader, threw);
        }
    }

    private void sendPayload(Payload payload, HttpURLConnection urlConnection) throws IOException
    {
        urlConnection.connect();
        OutputStream outputStream = urlConnection.getOutputStream();
        Writer writer = new OutputStreamWriter(outputStream, Charsets.UTF_8);
        boolean threw = true;
        try
        {
            payload.writeTo(writer);
            threw = false;
        }
        finally
        {
            Closeables.close(writer, threw);
        }
    }

    private void configure(HttpURLConnection urlConnection) throws ProtocolException
    {
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", contentType);
        urlConnection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(10));
        urlConnection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(30));
    }

    private HttpURLConnection buildConnection() throws IOException
    {
        if (endpoint == null)
            throw new IllegalArgumentException("no endpoint is configured!");

        return (HttpURLConnection) endpoint.toURL().openConnection();
    }
}
