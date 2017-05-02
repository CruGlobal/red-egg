package org.cru.redegg.boot;

import org.cru.redegg.recording.api.LoggingRecorder;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.log4j.Log4jRecorder;

import java.util.Set;

public class Log4jLogging
{
    public static boolean isAvailable()
    {
        try
        {
            Log4jLogging.class.getClassLoader().loadClass("org.apache.log4j.AppenderSkeleton");
            return true;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }

    public static LoggingRecorder addLog4jAppender(
        RecorderFactory recorderFactory,
        Set<String> ignoredLoggers)
    {
        return Log4jRecorder.add(recorderFactory, ignoredLoggers);
    }
}
