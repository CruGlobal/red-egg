package org.cru.redegg.manual;

import org.cru.redegg.boot.Lifecycle;
import org.cru.redegg.jaxrs.RecordingReaderInterceptor;
import org.cru.redegg.recording.api.EntitySanitizer;
import org.cru.redegg.recording.api.ParameterSanitizer;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.Serializer;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.recording.gson.GsonSerializer;
import org.cru.redegg.recording.impl.DefaultErrorRecorder;
import org.cru.redegg.recording.impl.DefaultWebErrorRecorder;
import org.cru.redegg.recording.impl.HyperConservativeEntitySanitizer;
import org.cru.redegg.recording.impl.HyperConservativeParameterSanitizer;
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
     * Start off the app with a very conservative parameter sanitizer,
     * because we don't know at what point the app will do its own initialization logic
     * that installs its own custom parameter sanitizer.
     * That logic may not execute until the first request comes in.
     * By this point, we need to have a parameter sanitizer in place already,
     * and since we don't know what is sensitive and what is not,
     * so we will just assume everything is sensitive.
     */
    private final ReplaceableParameterSanitizer parameterSanitizer =
        new ReplaceableParameterSanitizer(new HyperConservativeParameterSanitizer());


    /**
     * Start off with a very conservative entity sanitizer, as {@link #parameterSanitizer} does.
     */
    private final ReplaceableEntitySanitizer entitySanitizer =
        new ReplaceableEntitySanitizer(new HyperConservativeEntitySanitizer());

    private volatile ErrbitConfig errbitConfig;
    private volatile InMemoryErrorQueue queue;

    public void init(RedEggServletListener listener)
    {
        listener.setCategorizer(buildParameterCategorizer());
        listener.setClock(Clock.system());
        listener.setLifecycle(buildLifecycle());
        listener.setRecorderFactory(buildRecorderFactory());
        listener.setSanitizer(parameterSanitizer);
    }

    ParameterCategorizer buildParameterCategorizer()
    {
        return new ParameterCategorizer(parameterSanitizer);
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
        /* ManualRecorderFactory  is stateless; multiple instances are fine */
        return new ManualRecorderFactory(this);
    }

    WebErrorRecorder buildWebRecorder()
    {
        return new DefaultWebErrorRecorder(
            buildDefaultErrorRecorder(),
            buildQueue(),
            buildErrorLog(),
            entitySanitizer);
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
        shutdownQueue();
    }

    public void setParameterSanitizer(ParameterSanitizer sanitizer)
    {
        this.parameterSanitizer.replace(sanitizer);
    }

    public void setEntitySanitizer(EntitySanitizer sanitizer)
    {
        this.entitySanitizer.replace(sanitizer);
    }

    public void setErrbitConfig(ErrbitConfig errbitConfig)
    {
        /*
         * The current queue, if it exists, is using the old ErrbitConfig. It needs to go.
         * A new queue (with the correct config) will be created when needed.
         */
        shutdownQueue();
        this.errbitConfig = errbitConfig;
    }

    private void shutdownQueue()
    {
        if (queue != null)
        {
            queue.shutdown();
            queue = null;
        }
    }

    public RecorderFactory getRecorderFactory()
    {
        return buildRecorderFactory();
    }

    public void init(RecordingReaderInterceptor interceptor)
    {
        interceptor.setFactory(buildRecorderFactory());
    }
}
