package org.cru.redegg.recording.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import org.cru.redegg.qualifier.Selected;
import org.cru.redegg.recording.StuckThreadMonitor;
import org.cru.redegg.recording.StuckThreadMonitorConfig;
import org.cru.redegg.recording.api.NotificationLevel;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.cru.redegg.reporting.api.ErrorQueue;
import org.cru.redegg.util.ErrorLog;
import org.cru.redegg.util.MoreExecutors;
import org.cru.redegg.util.ProxyConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class DefaultStuckThreadMonitor implements StuckThreadMonitor
{

    private static final Logger LOG = LoggerFactory.getLogger(DefaultStuckThreadMonitor.class);

    /*
     * Use a weak-keys map so that, on the off chance that a thread doesn't call
     * finishMonitoringRequest() before it finishes a request
     * (some kind of Error might be thrown, for example),
     * the map entry will eventually get garbage collected.
     */
    private final Map<WebContext, Request> currentlyActiveRequests = new MapMaker()
        .initialCapacity(50)
        .weakKeys()
        .makeMap();

    private final ScheduledExecutorService executorService =
        new ScheduledThreadPoolExecutor(1);


    private TemporalAmount threshold;
    private long period;
    private TimeUnit periodTimeUnit;

    private InetAddress localHost;
    {
        try
        {
            localHost = InetAddress.getLocalHost();
        }
        catch (UnknownHostException e)
        {
            LOG.warn("can't get localhost", e);
        }
    }


    private final Clock clock;
    private final ErrorQueue errorQueue;
    private final ErrorLog errorLog;

    @Inject
    public DefaultStuckThreadMonitor(
        @Selected Clock clock,
        ErrorQueue errorQueue,
        @Selected StuckThreadMonitorConfig config,
        ErrorLog errorLog)
    {
        this.clock = clock;
        this.errorQueue = errorQueue;
        this.threshold = config.getThreshold();
        this.period = config.getPeriod();
        this.periodTimeUnit = config.getPeriodTimeUnit();
        this.errorLog = errorLog;
    }

    @ProxyConstructor
    public DefaultStuckThreadMonitor()
    {
        this.clock = null;
        this.errorQueue = null;
        this.errorLog = null;
    }

    @PostConstruct
    public void start()
    {
        executorService.scheduleAtFixedRate(
            new Scanner(),
            period,
            period,
            periodTimeUnit);
    }

    @Override
    public void startMonitoringRequest(WebContext webContext)
    {
        Request request = new Request(
            webContext.getStart().plus(threshold),
            webContext.clone(),
            Thread.currentThread());

        currentlyActiveRequests.put(webContext, request);
    }

    @Override
    public void finishMonitoringRequest(WebContext webContext)
    {
        currentlyActiveRequests.remove(webContext);
    }

    @PreDestroy
    public void stop()
    {
        MoreExecutors.shutdownAndHandleInterruptions(
            executorService,
            5,
            "stuck thread monitor",
            errorLog
        );
    }


    private class Request {
        final Instant deadline;
        final WebContext webContext;
        final Thread processingThread;
        final AtomicBoolean notified = new AtomicBoolean();

        Request(
            Instant deadline,
            WebContext webContext,
            Thread processingThread)
        {
            this.deadline = deadline;
            this.webContext = webContext;
            this.processingThread = processingThread;
        }
    }

    private class Scanner implements Runnable
    {
        @Override
        public void run()
        {
            Instant now = Instant.now(clock);
            for (Map.Entry<WebContext, Request> entry : currentlyActiveRequests.entrySet())
            {
                Request request = entry.getValue();
                boolean overdue = now.isAfter(request.deadline);
                if (overdue && !request.notified.get())
                {
                    request.notified.set(true);
                    reportOverdue(request, request.webContext, now);
                }
            }
        }

        private void reportOverdue(
            Request request,
            WebContext webContext,
            Instant now)
        {
            errorQueue.enqueue(buildReport(request, webContext, now));
        }

        private ErrorReport buildReport(
            Request request,
            WebContext webContext,
            Instant now)
        {
            ErrorReport report = new ErrorReport();
            report.setWebContext(webContext);
            report.setErrorLink(errorQueue.buildLink().orElse(null));

            Throwable standInException = buildStandInException(webContext, request, now);
            report.setThrown(Collections.singletonList(standInException));

            report.setNotificationLevel(NotificationLevel.ERROR);
            if (localHost != null)
            {
                report.setLocalHostName(localHost.getHostName());
                report.setLocalHostAddress(localHost.getHostAddress());
            }

            return report;
        }

        private Throwable buildStandInException(
            WebContext webContext,
            Request request,
            Instant now)
        {
            return new StuckThreadException(
                webContext.getStart(),
                now,
                request.processingThread);
        }
    }

    /**
     * An exception that marks the location of a stuck request-processing thread.
     * This exception isn't actually created from the location where it appears to have been created
     * (looking at its stacktrace.)
     *
     * Rather, it is stacktrace is copied from the stuck thread's current stack trace.
     * We use an exception to encapsulate this
     * to make it easier to integrate with exception-reporting tools.
     */
    public static class StuckThreadException extends RuntimeException
    {

        StuckThreadException(
            Instant start,
            Instant now,
            Thread thread)
        {
            super(String.format(
                "Request thread %s seems to be stuck; the request began %s ago",
                thread.getName(),
                getRelativeTimePhrase(now, start)));

            setStackTrace(thread.getStackTrace());
        }

        private static String getRelativeTimePhrase(
            Instant now,
            Instant start)
        {
            Preconditions.checkArgument(start.isBefore(now));
            Duration period = Duration.between(start, now);
            return period.toString();
        }

        // don't fill in the current thread's stack trace; see constructor
        @Override
        public synchronized Throwable fillInStackTrace()
        {
            return this;
        }
    }
}
