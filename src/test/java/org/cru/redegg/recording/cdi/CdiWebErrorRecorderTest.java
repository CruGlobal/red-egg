package org.cru.redegg.recording.cdi;

import org.cru.redegg.recording.api.Serializer;
import org.cru.redegg.recording.impl.DefaultErrorRecorder;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.api.ErrorQueue;
import org.cru.redegg.util.ErrorLog;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Matt Drees
 */
public class CdiWebErrorRecorderTest
{

    CdiWebErrorRecorder recorder;

    @Mock
    ErrorQueue queue;

    @Mock
    ErrorLog errorLog;

    @Mock
    Serializer serializer;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        recorder = new CdiWebErrorRecorder();
        recorder.queue = queue;
        recorder.errorLog = errorLog;
        recorder.defaultRecorder = new DefaultErrorRecorder(queue, serializer);
    }

    @Test
    public void anErrorShouldTriggerAReport()
    {
        recorder.recordThrown(new InternalServerErrorException());
        recorder.recordResponseStatus(500);
        recorder.recordRequestComplete(new DateTime());

        verify(queue).enqueue(any(ErrorReport.class));
    }


    @Test
    public void clientErrorShouldNotTriggerAReport()
    {
        recorder.recordThrown(new BadRequestException());
        recorder.recordResponseStatus(400);
        recorder.recordRequestComplete(new DateTime());

        verify(queue, never()).enqueue(any(ErrorReport.class));
    }

    @Test
    public void clientErrorShouldTriggerAReportWhenErrorWasManuallyCalled()
    {
        recorder.recordThrown(new BadRequestException());
        recorder.recordResponseStatus(400);
        recorder.error();
        recorder.recordRequestComplete(new DateTime());

        verify(queue).enqueue(any(ErrorReport.class));
    }

}
