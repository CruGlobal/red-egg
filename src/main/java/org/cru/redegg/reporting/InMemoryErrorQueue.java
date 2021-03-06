package org.cru.redegg.reporting;

import com.google.common.base.Throwables;
import org.cru.redegg.qualifier.Fallback;
import org.cru.redegg.qualifier.Selected;
import org.cru.redegg.reporting.api.ErrorLink;
import org.cru.redegg.reporting.api.ErrorQueue;
import org.cru.redegg.reporting.api.ErrorReporter;
import org.cru.redegg.util.ErrorLog;
import org.cru.redegg.util.MoreExecutors;
import org.cru.redegg.util.ProxyConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Matt Drees
 */
@ApplicationScoped
public class InMemoryErrorQueue implements ErrorQueue
{

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryErrorQueue.class);

    private final ErrorReporter primaryErrorReporter;

    private final ErrorReporter fallbackReporter;

    private final ErrorLog errorLog;

    private final ExecutorService executorService;

    private final DatadogEnricher enricher;

    @Inject
    public InMemoryErrorQueue(
        @Selected ErrorReporter primaryErrorReporter,
        @Fallback ErrorReporter fallbackReporter,
        ErrorLog errorLog,
        DatadogEnricher enricher)
    {
        this(
            primaryErrorReporter,
            fallbackReporter,
            errorLog,
            enricher,
            //TODO: this should probably be configurable
            new ThreadPoolExecutor(
                0,
                10,
                5,
                TimeUnit.MINUTES,
                new ArrayBlockingQueue<Runnable>(1000))
        );
    }

    @ProxyConstructor
    @SuppressWarnings("UnusedDeclaration")
    InMemoryErrorQueue() {
        this(null, null, null, null);
    }

    InMemoryErrorQueue(
        ErrorReporter primaryErrorReporter,
        ErrorReporter fallbackReporter,
        ErrorLog errorLog,
        DatadogEnricher enricher,
        ExecutorService executorService)
    {
        this.primaryErrorReporter = primaryErrorReporter;
        this.fallbackReporter = fallbackReporter;
        this.errorLog = errorLog;
        this.enricher = enricher;
        this.executorService = executorService;
    }

    @PreDestroy
    public void shutdown()
    {
        MoreExecutors.shutdownAndHandleInterruptions(
            executorService,
            20,
            "report",
            errorLog
        );
    }

    @Override
    public void enqueue(final ErrorReport report)
    {
        try
        {
            submit(report);
            final ErrorLink errorLink = report.getErrorLink();
            if (errorLink != null)
            {
                LOG.info("Error details available at {}", errorLink.getTarget());
            }
        }
        catch (RejectedExecutionException e)
        {
            errorLog.error("unable to submit error report to queue; using fallback reporter", e);

            // run on this thread
            fallback(report);
        }
    }

    private void submit(final ErrorReport report)
    {
        executorService.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    enricher.enrich(report);
                    primaryErrorReporter.send(report);
                }
                catch (Throwable t)
                {
                    errorLog.error("unable to send error report; using fallback reporter", t);
                    fallback(report);

                    //propagate to kill this thread if necessary
                    throw Throwables.propagate(t);
                }
            }
        });
    }

    private void fallback(ErrorReport report)
    {
        try
        {
            fallbackReporter.send(report);
        }
        catch (Throwable t2)
        {
            errorLog.error("unable to send error report with fallback reporter", t2);
            //swallow t2
        }
    }

    @Override
    public Optional<ErrorLink> buildLink()
    {
        return primaryErrorReporter.buildLink();
    }
}
