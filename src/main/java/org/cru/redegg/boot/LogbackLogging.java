package org.cru.redegg.boot;


import org.cru.redegg.recording.api.LoggingRecorder;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.logback.LogbackRecorder;

import java.util.Set;

/**
 * @author Matt Drees
 */
public class LogbackLogging
{
    public static boolean isAvailable()
    {
        try
        {
            LogbackLogging.class.getClassLoader().loadClass("ch.qos.logback.core.UnsynchronizedAppenderBase");
            return LogbackRecorder.isRootSlf4jLoggerInstanceofLogbackLogger();
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }

    public static LoggingRecorder addLogbackAppender(
        RecorderFactory recorderFactory,
        Set<String> ignoredLoggers)
    {
        return LogbackRecorder.add(recorderFactory, ignoredLoggers);
    }
}
