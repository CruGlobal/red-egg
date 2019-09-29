package org.cru.redegg.jaxrs;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import org.cru.redegg.boot.Initializer;
import org.cru.redegg.manual.ManualRecorderFactory;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.util.ErrorLog;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author Matt Drees
 */
@Provider
public class RecordingReaderInterceptor implements ReaderInterceptor
{

    @Inject
    RecorderFactory recorderFactory;

    @Inject
    ErrorLog errorLog;

    @Context
    ServletContext servletContext;



    @PostConstruct
    public void init()
    {
        Initializer.initializeIfNecessary(this, servletContext);
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException
    {
        //for now, we buffer all request entities.
        //FUTURE: enable a way to opt-out of this, if requests are too massive to be buffered in memory
        //TODO: add a note in the docs about this

        byte[] content = ByteStreams.toByteArray(context.getInputStream());
        recordContentIfPossible(context, content);
        context.setInputStream(new ByteArrayInputStream(content));
        return context.proceed();
    }

    private void recordContentIfPossible(ReaderInterceptorContext context, byte[] content)
    {
        try
        {
            Charset charsetGuess = guessCharset(context);
            String representation = new String(content, charsetGuess);

            WebErrorRecorder recorder = recorderFactory.getWebRecorder();
            recorder.recordEntityRepresentation(representation);
        }
        catch (Throwable throwable)
        {
            errorLog.error("unable to record entity", throwable);
        }
    }

    /**
     * I think this is pretty tricky to do right.
     * This is a rough attempt that will probably work often enough for Cru.
     */
    private Charset guessCharset(ReaderInterceptorContext context)
    {
        String charsetFromMediaType = context.getMediaType().getParameters().get(MediaType.CHARSET_PARAMETER);
        Charset defaultGuess = Charsets.UTF_8;
        if (charsetFromMediaType == null)
            return defaultGuess;

        return Charset.forName(charsetFromMediaType);
    }

    public void setFactory(RecorderFactory recorderFactory)
    {
        this.recorderFactory = recorderFactory;
    }
}
