package org.cru.redegg.recording.jul;

import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.util.ErrorLog;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Matt Drees
 */
public class RedEggHandler extends Handler {

    private final RecorderFactory factory;

    public RedEggHandler(RecorderFactory factory) {
        this.factory = checkNotNull(factory);
        setLevel(Level.FINE);
    }

    @Override
    public void publish(LogRecord record) {
        if (record.getLoggerName().equals(ErrorLog.name()))
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
