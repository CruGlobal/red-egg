package org.cru.redegg.boot;

import org.cru.redegg.recording.api.LoggingRecorder;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.jul.JulRecorder;

import java.util.Set;

/**
 * @author Matt Drees
 */
public class JulLogging
{
    public static LoggingRecorder addJulHandler(
        RecorderFactory recorderFactory,
        Set<String> ignoredLoggers)
    {
        return JulRecorder.add(recorderFactory, ignoredLoggers);
    }
}
