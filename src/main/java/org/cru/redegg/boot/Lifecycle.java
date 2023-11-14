package org.cru.redegg.boot;

import com.google.common.collect.ImmutableSet;
import org.cru.redegg.recording.api.LoggingRecorder;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.reporting.LoggingReporter;
import org.cru.redegg.util.ErrorLog;
import org.cru.redegg.util.ProxyConstructor;
import org.cru.redegg.util.RedEggVersion;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;

/**
 * @author Matt Drees
 */
@ApplicationScoped
public class Lifecycle
{
    private final RecorderFactory recorderFactory;

    /**
     * The logger names for which 'error()' calls should *not* trigger a notification.
     * If they did, we could get into an infinite loop.
     */
    private final Set<String> ignoredLoggers = ImmutableSet.of(
        ErrorLog.name(),
        LoggingReporter.name(),

        // TODO: It'd be be more architecturally clean for this to be added
        // by some rollbar-specific code, but it'll do for now.
        "com.rollbar.notifier.Rollbar",
        "com.rollbar.notifier.sender.SyncSender",
        "com.rollbar.notifier.sender.BufferedSender",
        "com.rollbar.notifier.sender.queue.DiskQueue",
        "com.rollbar.notifier.util.ObjectsUtils",
        "ConfigProviderHelper" // used in com.rollbar.notifier.config.ConfigProviderHelper
    );

    @Inject
    public Lifecycle(RecorderFactory recorderFactory)
    {
        this.recorderFactory = recorderFactory;
    }

    @ProxyConstructor
    @SuppressWarnings("UnusedDeclaration")
    Lifecycle()
    {
        recorderFactory = null;
    }

    private LoggingRecorder recorder;

    public void beginApplication()
    {
        // load this eagerly since its xpath usage can get thrown off by TCCL changes
        RedEggVersion.get();

        if (Log4j2Logging.isAvailable())
        {
            recorder = Log4j2Logging.addLog4j2Appender(recorderFactory, ignoredLoggers);
        }
        else if (LogbackLogging.isAvailable())
        {
            recorder = LogbackLogging.addLogbackAppender(recorderFactory, ignoredLoggers);
        }
        else if (Log4jLogging.isAvailable())
        {
            recorder = Log4jLogging.addLog4jAppender(recorderFactory, ignoredLoggers);
        }
        else // J.U.L. is always available
        {
            recorder = JulLogging.addJulHandler(recorderFactory, ignoredLoggers);
        }
    }

    public void endApplication()
    {
        if (recorder != null)
        {
            recorder.remove();
            recorder = null;
        }
    }

    public void beginRequest()
    {
    }

    public void endRequest()
    {
    }

}
