package org.cru.redegg.reporting;

import org.cru.redegg.reporting.api.ErrorReporter;
import org.cru.redegg.util.ErrorLog;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Matt Drees
 */
public class InMemoryErrorQueueTest
{
    @Mock
    ErrorReporter primaryErrorReporter;

    @Mock
    ErrorReporter fallbackReporter;

    @Mock
    ErrorLog errorLog;

    @Mock
    ExecutorService executorService;

    @Mock
    DatadogEnricher enricher;

    InMemoryErrorQueue queue;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        queue = new InMemoryErrorQueue(
            primaryErrorReporter,
            fallbackReporter,
            errorLog,
            enricher,
            executorService);
    }

    @Test
    public void testEnqueue() throws Exception
    {
        queue.enqueue(new ErrorReport());

        verify(executorService).submit(any(Runnable.class));
        verify(errorLog, never()).error(anyString(), any(Throwable.class));
    }

    @Test
    public void testEnqueueWhenExecutorServiceRejectsTask() throws Exception
    {
        when(executorService.submit(any(Runnable.class)))
            .thenThrow(new RejectedExecutionException());

        ErrorReport report = new ErrorReport();
        queue.enqueue(report);

        verify(fallbackReporter).send(report);
        verify(errorLog).error(anyString(), any(Throwable.class));
    }
}
