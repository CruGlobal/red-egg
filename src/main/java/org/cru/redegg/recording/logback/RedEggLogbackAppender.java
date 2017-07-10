package org.cru.redegg.recording.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.RecorderFactory;

import java.util.Map;
import java.util.Set;
import java.util.logging.LogRecord;

/**
 * @author Matt Drees
 */
public class RedEggLogbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent>
{
    private final RecorderFactory factory;
    private final Set<String> ignoredLoggerNames;

    public RedEggLogbackAppender(
        RecorderFactory factory,
        Set<String> ignoredLoggerNames)
    {
        this.factory = factory;
        this.ignoredLoggerNames = ignoredLoggerNames;
    }

    @Override
    protected void append(ILoggingEvent event)
    {
        if (ignoredLoggerNames.contains(event.getLoggerName()))
            return;

        ErrorRecorder recorder = factory.getRecorder();

        if (event.getLevel().toInt() >= Level.WARN.toInt()) {
            Map<String, String> properties = event.getMDCPropertyMap();
            for (Map.Entry<String, String> entry : properties.entrySet())
            {
                recorder.recordContext(entry.getKey(), entry.getValue());
            }
        }

        recorder
            .recordLogRecord(toLogRecord(event))
            .sendReportIfNecessary();
    }

    private LogRecord toLogRecord(ILoggingEvent event)
    {
        LogRecord record = new LogRecord(
            toJulLevel(event.getLevel()),
            String.valueOf(event.getFormattedMessage()));

        IThrowableProxy iThrowbleProxy = event.getThrowableProxy();
        if (iThrowbleProxy != null &&
            iThrowbleProxy instanceof ThrowableProxy)
        {
            ThrowableProxy throwbleProxy = (ThrowableProxy) iThrowbleProxy;
            record.setThrown(throwbleProxy.getThrowable());
        }

        record.setMillis(event.getTimeStamp());
        record.setLoggerName(event.getLoggerName());
        StackTraceElement callerElement = event.getCallerData()[0];
        record.setSourceClassName(callerElement.getClassName());
        record.setSourceMethodName(callerElement.getMethodName());
        return record;
    }

    private java.util.logging.Level toJulLevel(Level level) {
        switch (level.toInt()) {
            case Level.ALL_INT:
                return java.util.logging.Level.ALL;
            case Level.TRACE_INT:
                return java.util.logging.Level.FINEST;
            case Level.DEBUG_INT:
                return java.util.logging.Level.FINE;
            case Level.INFO_INT:
                return java.util.logging.Level.INFO;
            case Level.ERROR_INT:
                return java.util.logging.Level.SEVERE;
            case Level.WARN_INT:
                return java.util.logging.Level.WARNING;
            case Level.OFF_INT:
                return java.util.logging.Level.OFF;
            default:
                throw new UnsupportedOperationException("custom logback levels aren't currently supported");
        }

    }

}
