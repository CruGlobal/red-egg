package org.cru.redegg.servlet;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.util.ErrorLog;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;

/**
 * @author Matt Drees
 *
 * For now, we buffer all request entities.
 *
 * FUTURE: enable a way to opt-out of this, if requests are too massive to be buffered in memory
 */
public class RecordingRequest extends HttpServletRequestWrapper
{

    private final ErrorLog errorLog;
    private final WebErrorRecorder recorder;

    public RecordingRequest(HttpServletRequest request, ErrorLog errorLog, WebErrorRecorder recorder)
    {
        super(request);
        this.errorLog = errorLog;
        this.recorder = recorder;
    }

    @Override
    public BufferedReader getReader() throws IOException
    {
        String entity = CharStreams.toString(super.getReader());
        recorder.recordEntityRepresentation(entity);
        return new BufferedReader(new StringReader(entity));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        String characterEncoding = getCharacterEncoding();
        String contentEncoding = getHeader("Content-Encoding");
        boolean isContentEncoded = contentEncoding != null && !contentEncoding.equals("identity");
        if (characterEncoding == null || isContentEncoded)
        {
            return super.getInputStream();
        }
        else
        {
            Charset charset;
            try
            {
                charset = Charset.forName(characterEncoding);
            }
            catch (IllegalArgumentException e)
            {
                return super.getInputStream();
            }
            return recordEntityIfPossibleAndWrapContent(charset);
        }
    }

    private ServletInputStream recordEntityIfPossibleAndWrapContent(Charset charset) throws IOException
    {
        byte[] content = ByteStreams.toByteArray(super.getInputStream());
        try
        {
            String entity = new String(content, charset);
            recorder.recordEntityRepresentation(entity);
        }
        catch (Throwable throwable)
        {
            errorLog.error("unable to record entity", throwable);
        }
        return new SimpleServletInputStream(new ByteArrayInputStream(content));
    }

}
