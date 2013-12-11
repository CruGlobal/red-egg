package org.cru.redegg.reporting;

import com.google.common.base.Throwables;
import org.cru.redegg.reporting.api.ErrorQueue;
import org.cru.redegg.reporting.api.ErrorReporter;
import org.cru.redegg.reporting.api.Fallback;
import org.cru.redegg.util.Log;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Matt Drees
 */
@ApplicationScoped
public class InMemoryErrorQueue implements ErrorQueue
{

    //TODO: this should probably be configurable
    ExecutorService executorService = new ThreadPoolExecutor(
        0,
        10,
        5,
        TimeUnit.MINUTES,
        new ArrayBlockingQueue<Runnable>(1000));

    @Inject
    ErrorReporter primaryErrorReporter;

    @Inject @Fallback
    ErrorReporter fallbackReporter;

    @Inject
    Log log;

    @PreDestroy
    public void shutdown() throws InterruptedException
    {
        executorService.shutdown();
        executorService.awaitTermination(20, TimeUnit.SECONDS);
    }

    @Override
    public void enqueue(final ErrorReport report)
    {
        executorService.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    primaryErrorReporter.send(report);
                }
                catch (Throwable t)
                {
                    log.error("unable to send error report; using fallback reporter", t);

                    try
                    {
                        fallbackReporter.send(report);
                    }
                    catch (Throwable t2)
                    {
                        log.error("unable to send error report with fallback reporter", t2);
                        //swallow t2
                    }

                    //propagate to kill this thread if necessary
                    throw Throwables.propagate(t);
                }
            }
        });
    }
}
