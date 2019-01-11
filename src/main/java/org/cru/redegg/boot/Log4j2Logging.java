package org.cru.redegg.boot;

import org.cru.redegg.recording.api.LoggingRecorder;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.log4j2.Log4j2Recorder;

import java.util.Set;

public class Log4j2Logging
{
    public static boolean isAvailable()
    {
        try
        {
            Log4j2Logging.class.getClassLoader().loadClass("org.apache.logging.log4j.core.appender.AbstractAppender");
            return true;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }

    public static LoggingRecorder addLog4j2Appender(
        RecorderFactory recorderFactory,
        Set<String> ignoredLoggers)
    {
        return Log4j2Recorder.add(recorderFactory, ignoredLoggers);
    }
}
