package org.cru.redegg.recording.jul;

import com.google.common.base.Preconditions;
import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.RecorderFactory;

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
        setLevel(Level.SEVERE);
    }

    @Override
    public void publish(LogRecord record) {
        if (record.getLevel().intValue() >= getLevel().intValue())
        {
//            System.out.println("Got a j.u.l error record: " + record.getMessage());
            ErrorRecorder recorder = factory.getRecorder();
            recorder
                .recordLogRecord(record)
                .error();
        }

    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
