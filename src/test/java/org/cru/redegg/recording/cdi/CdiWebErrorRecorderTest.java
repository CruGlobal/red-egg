package org.cru.redegg.recording.cdi;

import org.cru.redegg.recording.api.Serializer;
import org.cru.redegg.recording.impl.DefaultErrorRecorder;
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

import static org.mockito.Matchers.argThat;
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
        private boolean expectedUserError;

        public ErrorReportTypeSafeDiagnosingMatcher(boolean expectedUserError)
        {
            this.expectedUserError = expectedUserError;
        }

        @Override
        protected boolean matchesSafely(
            ErrorReport item, Description mismatchDescription)
        {
            if (item.isUserError() == expectedUserError) return true;
            else
            {
                mismatchDescription
                    .appendText("userError is ")
                    .appendValue(item.isUserError());
                return false;
            }
        }

        @Override
        public void describeTo(Description description)
        {
            description
                .appendText("a report whose userError property is ")
                .appendValue(expectedUserError);
        }
    }
}
