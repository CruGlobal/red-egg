package org.cru.redegg.recording.impl;

import org.cru.redegg.recording.api.Serializer;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.api.ErrorQueue;
import org.hamcrest.CoreMatchers;
import org.jboss.logmanager.Level;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.logging.LogRecord;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Matt Drees
 */
@SuppressWarnings("ThrowableInstanceNeverThrown")
public class DefaultErrorRecorderTest
{

    @Mock
    Serializer serializer;

    @Mock
    ErrorQueue queue;

    DefaultErrorRecorder recorder;


    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        recorder = new DefaultErrorRecorder(queue, serializer);
        setupDummyExceptions();
    }

    IOException root;
    RuntimeException wrapper;
    IllegalStateException unrelated;

    private void setupDummyExceptions()
    {
        root = new IOException("boom");
        wrapper = new RuntimeException(root);
        unrelated = new IllegalStateException("not related to the io exception");
    }

    @Test
    public void testReportWithOneLoggedException() throws Exception
    {
        LogRecord record = new LogRecord(Level.ERROR, "bad stuff");
        record.setThrown(root);
        recorder.recordLogRecord(record);
        ErrorReport report = recorder.buildReport();

        assertThat(report.getRootException().isPresent(), is(true));
        assertThat(report.getRootException().get(), CoreMatchers.<Throwable>sameInstance(root));
    }

    @Test
    public void testReportWithTwoUnrelatedLoggedExceptions() throws Exception
    {
        LogRecord record1 = new LogRecord(Level.ERROR, "bad stuff");
        record1.setThrown(root);
        recorder.recordLogRecord(record1);

        LogRecord record2 = new LogRecord(Level.ERROR, "more bad stuff");
        record2.setThrown(unrelated);
        recorder.recordLogRecord(record2);

        ErrorReport report = recorder.buildReport();

        assertThat(report.getRootException().isPresent(), is(true));
        assertThat(report.getRootException().get(), CoreMatchers.<Throwable>sameInstance(root));
    }

    @Test
    public void testReportWithTwoRelatedLoggedExceptions() throws Exception
    {
        LogRecord record1 = new LogRecord(Level.ERROR, "bad stuff");
        record1.setThrown(root);
        recorder.recordLogRecord(record1);

        LogRecord record2 = new LogRecord(Level.ERROR, "wrapping the bad stuff");
        record2.setThrown(wrapper);
        recorder.recordLogRecord(record2);

        ErrorReport report = recorder.buildReport();

        assertThat(report.getRootException().isPresent(), is(true));
        assertThat(report.getRootException().get(), CoreMatchers.<Throwable>sameInstance(wrapper));
    }

    @Test
    public void testReportWithALoggedExceptionAndThenARecordedException() throws Exception
    {
        LogRecord record = new LogRecord(Level.ERROR, "bad stuff");
        record.setThrown(root);
        recorder.recordLogRecord(record);

        recorder.recordThrown(unrelated);

        ErrorReport report = recorder.buildReport();

        assertThat(report.getRootException().isPresent(), is(true));
        assertThat(report.getRootException().get(), CoreMatchers.<Throwable>sameInstance(root));
    }

    @Test
    public void testReportWithARecordedExceptionAndThenALoggedException() throws Exception
    {
        recorder.recordThrown(unrelated);

        LogRecord record = new LogRecord(Level.ERROR, "bad stuff");
        record.setThrown(root);
        recorder.recordLogRecord(record);

        ErrorReport report = recorder.buildReport();

        assertThat(report.getRootException().isPresent(), is(true));
        assertThat(report.getRootException().get(), CoreMatchers.<Throwable>sameInstance(unrelated));
    }


}
