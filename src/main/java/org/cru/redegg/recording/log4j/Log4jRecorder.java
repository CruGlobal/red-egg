package org.cru.redegg.recording.log4j;

import org.apache.log4j.Logger;
import org.cru.redegg.recording.api.RecorderFactory;

import java.util.Enumeration;
import java.util.Set;

/**
 * Encapsulates a dependency on log4j.
 *
 * @author Matt Drees
 */
public class Log4jRecorder
{
    private Logger root;
    private RedEggAppender log4jAppender;

    public static Log4jRecorder add(RecorderFactory recorderFactory, Set<String> ignoredLoggers)
    {
        return new Log4jRecorder(recorderFactory, ignoredLoggers);
    }

    public Log4jRecorder(RecorderFactory recorderFactory, Set<String> ignoredLoggers)
    {
        log4jAppender = new RedEggAppender(recorderFactory, ignoredLoggers);
        root = Logger.getRootLogger();
        root.info("adding log4j appender");
        root.addAppender(log4jAppender);
        Enumeration allAppenders = root.getAllAppenders();
        boolean none = !allAppenders.hasMoreElements();
        if (none)
            root.info("log4j appenders appear to be disabled");
    }

    public void remove()
    {
        root.info("removing log4j appender");
        root.removeAppender(log4jAppender);
    }
}
