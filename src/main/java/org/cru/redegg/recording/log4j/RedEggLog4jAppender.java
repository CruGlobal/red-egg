package org.cru.redegg.recording.log4j;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.RecorderFactory;

import java.util.Set;
import java.util.logging.LogRecord;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Matt Drees
 */
public class RedEggLog4jAppender extends AppenderSkeleton {

    private final RecorderFactory factory;
    private final Set<String> ignoredLoggerNames;

    public RedEggLog4jAppender(RecorderFactory factory, Set<String> ignoredLoggerNames) {
        this.factory = checkNotNull(factory);
        this.ignoredLoggerNames = checkNotNull(ignoredLoggerNames);
        setThreshold(Level.DEBUG);
    }

    @Override
    protected void append(LoggingEvent event) {
        if (ignoredLoggerNames.contains(event.getLoggerName()))
            return;

        ErrorRecorder recorder = factory.getRecorder();

        if (event.getLevel().toInt() >= Level.ERROR.toInt()) {
            recorder
                .recordContext("log4j MDC", event.getProperties())
                .recordContext("log4j NDC", event.getNDC());
        }

        recorder
            .recordLogRecord(toLogRecord(event))
            .sendReportIfNecessary();
    }

    private LogRecord toLogRecord(LoggingEvent event) {

        LogRecord record = new LogRecord(toJulLevel(event.getLevel()), String.valueOf(event.getMessage()));

        ThrowableInformation throwableInformation = event.getThrowableInformation();
        if (throwableInformation != null && throwableInformation.getThrowable() != null)
        {
            record.setThrown(throwableInformation.getThrowable());
        }
        else if (event.getMessage() instanceof Throwable) {
            Throwable throwable = (Throwable) event.getMessage();
            record.setThrown(throwable);
        }

        record.setMillis(event.getTimeStamp());
        record.setLoggerName(event.getLoggerName());
        record.setSourceClassName(event.getLocationInformation().getClassName());
        record.setSourceMethodName(event.getLocationInformation().getMethodName());
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
            case Level.FATAL_INT:
                return java.util.logging.Level.SEVERE;
            case Level.WARN_INT:
                return java.util.logging.Level.WARNING;
            case Level.OFF_INT:
                return java.util.logging.Level.OFF;
            default:
                throw new UnsupportedOperationException("custom log4j levels aren't currently supported");
        }

    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
