package org.cru.redegg.recording.impl;

import com.google.common.collect.Lists;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.cru.redegg.reporting.api.ErrorQueue;
import org.cru.redegg.util.Clock;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.joda.time.Period;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.compose;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.junit.Assert.assertThat;

/**
 * @author Matt Drees
 */
public class DefaultStuckThreadMonitorTest
{

    // 10 ms is too small; it causes fastRequestsAreNotNoticed() to fail occasionally
    private static final int TEST_THRESHOLD_MILLIS = 50;
    private static final int TEST_PERIOD_MILLIS = 5;

    DefaultStuckThreadMonitor monitor;

    Clock clock = Clock.system();

    ErrorQueueStub queue = new ErrorQueueStub();

    private static class ErrorQueueStub implements ErrorQueue {

        Set<ErrorReport> reports = new HashSet<>();

        @Override
        public void enqueue(ErrorReport report)
        {
            reports.add(report);
        }

        public void assertNothingEnqueued() throws InterruptedException
        {
            // sleep long enough that anything should be reported by now
            Thread.sleep(50);

            assertThat(reports, is(empty()));
        }

        public void assertSomethingEnqueued() throws InterruptedException
        {
            // sleep long enough that anything should be reported by now
            Thread.sleep(50);

            assertThat(reports, contains(expectedReport()));
        }

        private List<Matcher<? super ErrorReport>> expectedReport()
        {
            List<Matcher<? super ErrorReport>> list = Lists.newArrayList();
            Matcher<ErrorReport> matcher = compose("an error report with", hasFeature("one thrown", ErrorReport::getThrown, contains(instanceOf(DefaultStuckThreadMonitor.StuckThreadException.class))))

            list.add(matcher);

            return list;
        }
    }

    @Before
    public void setUp() throws Exception
    {
        monitor = new DefaultStuckThreadMonitor(clock, queue);

        monitor.threshold = Period.millis(TEST_THRESHOLD_MILLIS);
        monitor.period = TEST_PERIOD_MILLIS;
        monitor.periodTimeUnit = TimeUnit.MILLISECONDS;
        monitor.start();
    }

    @After
    public void tearDown() throws Exception
    {
        monitor.stop();
    }

    @Test
    public void fastRequestsAreNotNoticed() throws Exception
    {
        WebContext webContext = createWebContext();
        monitor.startMonitoringRequest(webContext);
        monitor.finishMonitoringRequest(webContext);

        queue.assertNothingEnqueued();
    }

    private WebContext createWebContext()
    {
        WebContext webContext = new WebContext();
        webContext.setStart(clock.dateTime());
        webContext.setUrl(URI.create("https://example.com/test"));
        return webContext;
    }

    @Test
    public void slowRequestsAreNoticed() throws Exception
    {
        WebContext webContext = createWebContext();
        monitor.startMonitoringRequest(webContext);
        Thread.sleep(TEST_THRESHOLD_MILLIS + TEST_PERIOD_MILLIS);
        monitor.finishMonitoringRequest(webContext);

        queue.assertNothingEnqueued();
    }



}