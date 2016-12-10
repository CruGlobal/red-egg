package org.cru.redegg.boot;

import com.google.common.collect.ImmutableSet;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.jul.JulRecorder;
import org.cru.redegg.recording.log4j.Log4jAvailability;
import org.cru.redegg.recording.log4j.Log4jRecorder;
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
        LoggingReporter.name()
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

    private Log4jRecorder log4JRecorder;
    private JulRecorder julRecorder;

    public void beginApplication()
    {
        // load this eagerly since its xpath usage can get thrown off by TCCL changes
        RedEggVersion.get();

        addLog4jAppender();
        addJulHandler();
    }

    public void endApplication()
    {
        if (log4JRecorder != null)
        {
            removeLog4jAppender();
        }
        removeJulHandler();
    }

    private void addLog4jAppender() {
        log4JRecorder = Log4jRecorder.add(recorderFactory, ignoredLoggers);
    }

    private void addJulHandler() {
        julRecorder = JulRecorder.add(recorderFactory, ignoredLoggers);
    }

    private void removeJulHandler()
    {
        julRecorder.remove();
        julRecorder = null;
    }

    private void removeLog4jAppender()
    {
        log4JRecorder.remove();
        log4JRecorder = null;
    }

    public void beginRequest()
    {
    }

    public void endRequest()
    {
    }

}
