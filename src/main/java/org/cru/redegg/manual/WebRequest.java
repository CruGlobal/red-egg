package org.cru.redegg.manual;

import org.cru.redegg.recording.api.WebErrorRecorder;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author Matt Drees
 */
public class WebRequest
{
    private static final ThreadLocal<WebRequest> request = new ThreadLocal<WebRequest>();

    private final WebErrorRecorder recorder;

    public WebRequest(WebErrorRecorder recorder)
    {
        this.recorder = recorder;
    }

    public static boolean isActive()
    {
        return request.get() != null;
    }

    public static WebRequest get()
    {
        checkState(isActive(), "there is no web request being processed on this thread");
        return request.get();
    }

    public static void begin(Builder builder)
    {
        request.set(new WebRequest(builder.buildWebRecorder()));
    }

    public static void end()
    {
        request.remove();
    }


    public WebErrorRecorder getRecorder()
    {
        return recorder;
    }
}
