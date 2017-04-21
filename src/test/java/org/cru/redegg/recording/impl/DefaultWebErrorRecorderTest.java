package org.cru.redegg.recording.impl;

import org.cru.redegg.recording.api.EntitySanitizer;
import org.cru.redegg.recording.api.NotificationLevel;
import org.cru.redegg.recording.api.Serializer;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.api.ErrorQueue;
import org.cru.redegg.util.ErrorLog;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;

import static org.cru.redegg.recording.api.NotificationLevel.ERROR;
import static org.cru.redegg.recording.api.NotificationLevel.WARNING;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;

/**
 * @author Matt Drees
 */
public class DefaultWebErrorRecorderTest
{

    DefaultWebErrorRecorder recorder;

    @Mock
    ErrorQueue queue;

    @Mock
    ErrorLog errorLog;

    @Mock
    Serializer serializer;

    @Mock
    EntitySanitizer entitySanitizer;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        recorder = new DefaultWebErrorRecorder(
            new DefaultErrorRecorder(queue, serializer),
            queue,
            errorLog,
            entitySanitizer);
    }

    @Test
    public void anErrorShouldTriggerAServerErrorReport()
    {
        recorder.recordThrown(new InternalServerErrorException());
        recorder.recordResponseStatus(500);
        recorder.recordRequestComplete(new DateTime());

        verify(queue).enqueue(argThat(isServerError()));
    }
    @Test
    public void clientErrorShouldTriggerAUserErrorReport()
    {
        recorder.recordThrown(new BadRequestException());
        recorder.recordResponseStatus(400);
        recorder.recordRequestComplete(new DateTime());

        verify(queue).enqueue(argThat(isUserError()));
    }

    @Test
    public void clientErrorShouldTriggerAUserErrorReportWhenErrorWasManuallyCalled()
    {
        recorder.recordThrown(new BadRequestException());
        recorder.recordResponseStatus(400);
        recorder.error();
        recorder.recordRequestComplete(new DateTime());

        verify(queue).enqueue(argThat(isUserError()));
    }

    private Matcher<ErrorReport> isServerError()
    {
        return new ErrorReportTypeSafeDiagnosingMatcher(false);
    }

    private Matcher<ErrorReport> isUserError()
    {
        return new ErrorReportTypeSafeDiagnosingMatcher(true);
    }


    private static class ErrorReportTypeSafeDiagnosingMatcher extends TypeSafeDiagnosingMatcher<ErrorReport>
    {
        private NotificationLevel expectedLevel;

        public ErrorReportTypeSafeDiagnosingMatcher(boolean expectedUserError)
        {
            this.expectedLevel = expectedUserError ? WARNING : ERROR;
        }

        @Override
        protected boolean matchesSafely(
            ErrorReport item, Description mismatchDescription)
        {
            NotificationLevel level = item.getNotificationLevel();
            if (level == expectedLevel) return true;
            else
            {
                mismatchDescription
                    .appendText("notificationLevel is ")
                    .appendValue(level);
                return false;
            }
        }

        @Override
        public void describeTo(Description description)
        {
            description
                .appendText("a report whose notificationLevel property is ")
                .appendValue(expectedLevel);
        }
    }
}
