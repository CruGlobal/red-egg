package org.cru.redegg.recording.impl;

import com.google.common.collect.Lists;
import org.cru.redegg.recording.StuckThreadMonitorConfig;
import org.cru.redegg.recording.api.NotificationLevel;
import org.cru.redegg.recording.impl.DefaultStuckThreadMonitor.StuckThreadException;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.cru.redegg.reporting.api.ErrorQueue;
import org.cru.redegg.util.Clock;
import org.cru.redegg.util.ErrorLog;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.compose;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeatureValue;

/**
 * @author Matt Drees
 */
public class DefaultStuckThreadMonitorTest
{

    // 50 ms is too small; it causes fastRequestsAreNotNoticed() to fail occasionally
    private static final int TEST_THRESHOLD_MILLIS = 100;
    private static final long TEST_PERIOD_MILLIS = 5L;

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
            waitALittle();
            assertThat(reports, is(empty()));
        }

        public void assertEnqueued(List<Matcher<? super ErrorReport>> expectedReport) throws InterruptedException
        {
            waitALittle();
            assertThat(reports, contains(expectedReport));
        }

        /** sleep long enough that anything should be reported by now */
        private void waitALittle() throws InterruptedException
        {
            Thread.sleep(TEST_THRESHOLD_MILLIS);
        }
    }

    @Before
    public void setUp()
    {
        StuckThreadMonitorConfig config =  new StuckThreadMonitorConfig();
        config.setThreshold(Period.millis(TEST_THRESHOLD_MILLIS));
        config.setPeriod(TEST_PERIOD_MILLIS);
        config.setPeriodTimeUnit(TimeUnit.MILLISECONDS);
        monitor = new DefaultStuckThreadMonitor(clock, queue, config, new ErrorLog());
        monitor.start();
    }

    @After
    public void tearDown()
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

        // Need to make sure the monitor scan runs at least once after the threshold is up.
        // Also, we need to make sure the monitor has enough time to capture this thread
        // while it is still at Thread.sleep(),
        // otherwise the test assertion will fail -- it will see a different frame at the top.
        // So we need a little more than just TEST_PERIOD_MILLIS by itself.
        // 15 milliseconds is not enough.
        long extra = 45;
        Thread.sleep(TEST_THRESHOLD_MILLIS + TEST_PERIOD_MILLIS + extra);

        monitor.finishMonitoringRequest(webContext);

        queue.assertEnqueued(expectedReport(webContext));
    }



    private List<Matcher<? super ErrorReport>> expectedReport(WebContext webContext)
    {
        List<Matcher<? super ErrorReport>> list = Lists.newArrayList();
        Matcher<ErrorReport> matcher = compose(
            "an error report with",
            hasFeature(
                "notification level",
                ErrorReport::getNotificationLevel,
                equalTo(NotificationLevel.ERROR))
        ).and(
            hasFeature(
                "thrown list",
                ErrorReport::getThrown,
                is(contains(isStuckThreadException())))
        ).and(
            hasFeature(
                "web context",
                ErrorReport::getWebContext,
                isDistinctCopyOf(webContext))
        );

        list.add(matcher);
        return list;
    }

    private Matcher<Throwable> isStuckThreadException()
    {
        return compose(
            "a throwable that",
            Matchers.<Throwable>is(instanceOf(StuckThreadException.class))
        ).and(
            hasFeature(
                "has a stack trace",
                Throwable::getStackTrace,
                thatIsSleeping())
        );
    }

    private Matcher<StackTraceElement[]> thatIsSleeping()
    {
        return new TypeSafeDiagnosingMatcher<StackTraceElement[]>()
        {
            @Override
            public void describeTo(Description description)
            {
                description.appendText("whose top method is Thread.sleep()");
            }

            @Override
            protected boolean matchesSafely(
                StackTraceElement[] elements, Description mismatchDescription)
            {
                if (elements.length == 0)
                {
                    mismatchDescription.appendText("is empty");
                    return false;
                }
                StackTraceElement topElement = elements[0];
                if (!topElement.getClassName().equals(Thread.class.getName()) ||
                    !topElement.getMethodName().equals("sleep"))
                {
                    mismatchDescription
                        .appendText("that is is not sleeping:")
                        .appendValueList("\n  ", "\n  ", "\n", elements);

                    return false;
                }

                return true;
            }
        };
    }

    private Matcher<WebContext> isDistinctCopyOf(WebContext webContext)
    {
        return compose(
            "with",
            hasFeatureValue(
                "start",
                WebContext::getStart,
                webContext.getStart())
        ).and(
            hasFeatureValue(
                "url",
                WebContext::getUrl,
                webContext.getUrl())
        ).and(
            // for threadsafe access, the reported WebContext is a distinct copy
            is(not(sameInstance(webContext)))
        );
    }


}