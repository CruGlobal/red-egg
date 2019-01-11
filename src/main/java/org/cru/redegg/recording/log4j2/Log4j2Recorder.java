package org.cru.redegg.recording.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.cru.redegg.recording.api.LoggingRecorder;
import org.cru.redegg.recording.api.RecorderFactory;

import java.util.Set;

/**
 * Encapsulates a dependency on log4j2.
 *
 * @author Matt Drees
 */
public class Log4j2Recorder implements LoggingRecorder
{
    private RedEggLog4j2Appender appender;
    private final LoggerContext loggerContext;

    public static Log4j2Recorder add(RecorderFactory recorderFactory, Set<String> ignoredLoggers)
    {
        return new Log4j2Recorder(recorderFactory, ignoredLoggers);
    }

    private Log4j2Recorder(RecorderFactory recorderFactory, Set<String> ignoredLoggers)
    {
        appender = new RedEggLog4j2Appender(recorderFactory, ignoredLoggers);
        LogManager.getRootLogger().info("adding log4j2 appender");
        loggerContext = (LoggerContext) LogManager.getContext(false);
        final Configuration config = loggerContext.getConfiguration();
        appender.start();
        config.addAppender(appender);
        LoggerConfig loggerConfig = config.getRootLogger();
        loggerConfig.addAppender(appender, Level.DEBUG, null);
        loggerContext.updateLoggers();
    }

    public void remove()
    {
        LogManager.getRootLogger().info("removing log4j2 appender");

        final AbstractConfiguration config = (AbstractConfiguration) loggerContext.getConfiguration();
        config.removeAppender(appender.getName());
        loggerContext.updateLoggers();
    }
}
