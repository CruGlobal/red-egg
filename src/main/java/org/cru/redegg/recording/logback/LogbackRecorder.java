package org.cru.redegg.recording.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.cru.redegg.recording.api.LoggingRecorder;
import org.cru.redegg.recording.api.RecorderFactory;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;

/**
 * Encapsulates a dependency on logback.
 *
 * @author Matt Drees
 */
public class LogbackRecorder implements LoggingRecorder
{
    private Logger root;
    private RedEggLogbackAppender appender;

    public static boolean isRootSlf4jLoggerInstanceofLogbackLogger()
    {
        return getRootSlf4jLogger() instanceof Logger;
    }

    public static LogbackRecorder add(RecorderFactory recorderFactory, Set<String> ignoredLoggers)
    {
        return new LogbackRecorder(recorderFactory, ignoredLoggers);
    }

    private LogbackRecorder(RecorderFactory recorderFactory, Set<String> ignoredLoggers)
    {
        appender = new RedEggLogbackAppender(recorderFactory, ignoredLoggers);
        root = (Logger) getRootSlf4jLogger();
        appender.setContext(root.getLoggerContext());
        appender.start();
        root.info("adding logback appender");
        root.addAppender(appender);
        Iterator<Appender<ILoggingEvent>> allAppenders = root.iteratorForAppenders();
        boolean none = !allAppenders.hasNext();
        if (none)
            root.info("logback appenders appear to be disabled");
    }

    private static org.slf4j.Logger getRootSlf4jLogger()
    {
        return LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }

    public void remove()
    {
        root.info("removing logback appender");
        root.detachAppender(appender);
    }
}
