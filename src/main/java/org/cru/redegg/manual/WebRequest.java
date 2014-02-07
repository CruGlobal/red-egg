package org.cru.redegg.manual;

import org.cru.redegg.recording.api.WebErrorRecorder;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author Matt Drees
 */
public class WebRequest
{
    private static final ThreadLocal<WebRequest> requests = new ThreadLocal<WebRequest>();

    private final WebErrorRecorder recorder;

    public WebRequest(WebErrorRecorder recorder)
    {
        this.recorder = recorder;
    }

    public static boolean isActive()
    {
        return requests.get() != null;
    }

    public static WebRequest get()
    {
        checkState(isActive(), "there is no web request being processed on this thread");
        return requests.get();
    }

    public static void begin(Builder builder)
    {
        requests.set(new WebRequest(builder.buildWebRecorder()));
    }

    public static void end()
    {
        requests.remove();
    }


    public WebErrorRecorder getRecorder()
    {
        return recorder;
    }
}
