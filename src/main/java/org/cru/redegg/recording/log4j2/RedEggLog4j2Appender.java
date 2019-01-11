package org.cru.redegg.recording.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.message.Message;
import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.RecorderFactory;

import java.util.Map;
import java.util.Set;
import java.util.logging.LogRecord;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Matt Drees
 */
public class RedEggLog4j2Appender extends AbstractAppender
{

    private final RecorderFactory factory;
    private final Set<String> ignoredLoggerNames;

    public RedEggLog4j2Appender(RecorderFactory factory, Set<String> ignoredLoggerNames) {
        super("red-egg-appender", null, null, false);
        this.factory = checkNotNull(factory);
        this.ignoredLoggerNames = checkNotNull(ignoredLoggerNames);
        addFilter(ThresholdFilter.createFilter(Level.DEBUG, Filter.Result.ACCEPT, Filter.Result.DENY));
    }

    @Override
    public void append(LogEvent event)
    {
        if (ignoredLoggerNames.contains(event.getLoggerName()))
            return;

        ErrorRecorder recorder = factory.getRecorder();

        if (event.getLevel().intLevel() >= Level.WARN.intLevel()) {

            Map<String, String> properties = event.getContextData().toMap();
            for (Map.Entry<String, String> entry : properties.entrySet())
            {
                recorder.recordContext(entry.getKey(), entry.getValue());
            }

            recorder.recordContext("log4j2 NDC", event.getContextStack().asList());
        }

        recorder
            .recordLogRecord(toLogRecord(event))
            .sendReportIfNecessary();

    }


    private LogRecord toLogRecord(LogEvent event) {
        Message message = event.getMessage();
        LogRecord record = new LogRecord(toJulLevel(event.getLevel()), message != null ? message.getFormattedMessage() : null);

        if (event.getThrown() != null)
        {
            record.setThrown(event.getThrown());
        }
        else if (event.getMessage().getThrowable() != null) { //TODO: check if this makes sense to do
            record.setThrown(event.getMessage().getThrowable());
        }

        record.setMillis(event.getTimeMillis());
        record.setLoggerName(event.getLoggerName());
        StackTraceElement source = event.getSource();
        record.setSourceClassName(source == null ? null : source.getClassName());
        record.setSourceMethodName(source == null ? null : source.getMethodName());
        return record;
    }

    private java.util.logging.Level toJulLevel(Level level) {
        if (level.intLevel() == Level.ALL.intLevel())
        {
            return java.util.logging.Level.ALL;
        }
        else if (level.intLevel() == Level.TRACE.intLevel())
        {
            return java.util.logging.Level.FINEST;
        }
        else if (level.intLevel() == Level.DEBUG.intLevel())
        {
            return java.util.logging.Level.FINE;
        }
        else if (level.intLevel() == Level.INFO.intLevel())
        {
            return java.util.logging.Level.INFO;
        }
        else if (level.intLevel() == Level.ERROR.intLevel())
        {
            return java.util.logging.Level.SEVERE;
        }
        else if (level.intLevel() == Level.FATAL.intLevel())
        {
            return java.util.logging.Level.SEVERE;
        }
        else if (level.intLevel() == Level.WARN.intLevel())
        {
            return java.util.logging.Level.WARNING;
        }
        else if (level.intLevel() == Level.OFF.intLevel())
        {
            return java.util.logging.Level.OFF;
        }
        throw new UnsupportedOperationException("custom log4j2 levels aren't currently supported");
    }
}
