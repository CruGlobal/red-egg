package org.cru.redegg.manual;

import org.cru.redegg.boot.Lifecycle;
import org.cru.redegg.recording.api.ParameterSanitizer;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.Serializer;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.recording.impl.DefaultWebErrorRecorder;
import org.cru.redegg.recording.gson.GsonSerializer;
import org.cru.redegg.recording.impl.DefaultErrorRecorder;
import org.cru.redegg.reporting.InMemoryErrorQueue;
import org.cru.redegg.reporting.LoggingReporter;
import org.cru.redegg.reporting.api.ErrorReporter;
import org.cru.redegg.reporting.errbit.ErrbitConfig;
import org.cru.redegg.reporting.errbit.NativeErrbitReporter;
import org.cru.redegg.servlet.ParameterCategorizer;
import org.cru.redegg.servlet.RedEggFilter;
import org.cru.redegg.servlet.RedEggServletListener;
import org.cru.redegg.util.Clock;
import org.cru.redegg.util.ErrorLog;

/**
 * performs manual dependency injection
 *
 * @author Matt Drees
 */
public class Builder
{
    private final static Builder INSTANCE = new Builder();

    public static Builder getInstance()
    {
        return INSTANCE;
    }

    /**
     * Start off the app with a very conservative sanitizer,
     * because we don't know at what point the app will do its own initialization logic
     * that installs its own custom sanitizer.
     * That logic may not execute until the first request comes in.
     * By this point, we need to have a sanitizer in place already,
     * and since we don't know what is sensitive and what is not,
     * so we will just assume everything is sensitive.
     */
    private final ReplaceableParameterSanitizer sanitizer = new ReplaceableParameterSanitizer(new HyperConservativeParameterSanitizer());
    private volatile ErrbitConfig errbitConfig;
    private volatile InMemoryErrorQueue queue;

    public void init(RedEggServletListener listener)
    {
        listener.setCategorizer(buildParameterCategorizer());
        listener.setClock(Clock.system());
        listener.setLifecycle(buildLifecycle());
        listener.setRecorderFactory(buildRecorderFactory());
        listener.setSanitizer(sanitizer);
    }

    ParameterCategorizer buildParameterCategorizer()
    {
        return new ParameterCategorizer(sanitizer);
    }

    Lifecycle buildLifecycle()
    {
        return new ManualLifecycle(buildRecorderFactory(), this);
    }

    public void init(RedEggFilter filter)
    {
        filter.setErrorLog(buildErrorLog());
        filter.setFactory(buildRecorderFactory());
    }

    ManualRecorderFactory buildRecorderFactory()
    {
        return new ManualRecorderFactory(this);
    }

    WebErrorRecorder buildWebRecorder()
    {
        return new DefaultWebErrorRecorder(
            buildDefaultErrorRecorder(),
            buildQueue(),
            buildErrorLog());
    }

    DefaultErrorRecorder buildDefaultErrorRecorder()
    {
        return new DefaultErrorRecorder(buildQueue(), buildSerializer());
    }

    Serializer buildSerializer()
    {
        return new GsonSerializer();
    }

    synchronized InMemoryErrorQueue buildQueue()
    {
        if (queue == null)
            queue = new InMemoryErrorQueue(
                buildPrimaryErrorReporter(),
                buildFallbackErrorReporter(),
                buildErrorLog());
        return queue;
    }

    private ErrorLog buildErrorLog()
    {
        return new ErrorLog();
    }

    ErrorReporter buildPrimaryErrorReporter()
    {
        if (errbitConfig == null)
            return buildFallbackErrorReporter();
        else
            return new NativeErrbitReporter(errbitConfig);
    }

    LoggingReporter buildFallbackErrorReporter()
    {
        return new LoggingReporter();
    }

    public synchronized void shutdown()
    {
        if (queue != null)
        {
            queue.shutdown();
            queue = null;
        }
    }

    public void setParameterSanitizer(ParameterSanitizer sanitizer)
    {
        this.sanitizer.replace(sanitizer);
    }

    public void setErrbitConfig(ErrbitConfig errbitConfig)
    {
        if (queue != null)
        {
            queue.shutdown();
            queue = null;
        }
        this.errbitConfig = errbitConfig;
    }

    public RecorderFactory getRecorderFactory()
    {
        return buildRecorderFactory();
    }
}
