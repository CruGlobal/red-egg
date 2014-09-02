package org.cru.redegg.recording.jul;

import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.util.ErrorLog;

import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Matt Drees
 */
public class RedEggHandler extends Handler {

    private final RecorderFactory factory;
    private final Set<String> ignoredLoggerNames;

    public RedEggHandler(RecorderFactory factory, Set<String> ignoredLoggerNames) {
        this.factory = checkNotNull(factory);
        this.ignoredLoggerNames = checkNotNull(ignoredLoggerNames);
        setLevel(Level.FINE);
    }

    @Override
    public void publish(LogRecord record) {
        if (ignoredLoggerNames.contains(record.getLoggerName()))
            return;
        ErrorRecorder recorder = factory.getRecorder();
        recorder
            .recordLogRecord(record)
            .sendReportIfNecessary();
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
