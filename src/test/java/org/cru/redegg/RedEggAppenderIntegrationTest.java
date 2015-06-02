package org.cru.redegg;


import org.cru.redegg.boot.Lifecycle;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.recording.jul.RedEggHandler;
import org.cru.redegg.recording.log4j.RedEggAppender;
import org.cru.redegg.reporting.LoggingReporter;
import org.cru.redegg.reporting.api.ErrorReporter;
import org.cru.redegg.servlet.ParameterCategorizer;
import org.cru.redegg.servlet.RedEggServletListener;
import org.cru.redegg.test.AnswerWithSelf;
import org.cru.redegg.test.DefaultDeployment;
import org.cru.redegg.util.Clock;
import org.cru.redegg.util.ErrorLog;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.logging.LogRecord;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@RunWith(Arquillian.class)
public class RedEggAppenderIntegrationTest
{
    @Deployment
    public static WebArchive deployment()  {

        return DefaultDeployment.withCdi()
            .addRecordingConfigurationClasses()
            .getArchive()
            .addClass(RedEggServletListener.class)
            .addClass(ParameterCategorizer.class)
            .addClass(RedEggHandler.class)
            .addClass(RedEggAppender.class)
            .addClass(ErrorLog.class)
            .addClass(Clock.class)
            .addPackage(Lifecycle.class.getPackage())

            .addPackage(RecorderFactory.class.getPackage())

            .addClass(LoggingReporter.class)
            .addClass(ErrorReporter.class)

            .addClass(AnswerWithSelf.class)
            .addClass(RecordingMocks.class);
    }


    @Inject
    WebErrorRecorder recorder;


    @Inject RecordingMocks mocks;

    @Before
    public void setup()
    {
        mocks.reset();
    }

    @Test
    public void testLog4jErrorLogging() throws Exception {
        org.apache.log4j.Logger.getRootLogger().error("error from test");

        verify(recorder, atLeast(1)).recordLogRecord(any(LogRecord.class));
        verify(recorder, atLeast(1)).sendReportIfNecessary();
    }

    @Test
    public void testJulErrorLogging() throws Exception {
        java.util.logging.Logger.getLogger("").severe("error from test");

        verify(recorder, atLeast(1)).recordLogRecord(any(LogRecord.class));
        verify(recorder, atLeast(1)).sendReportIfNecessary();
    }


}
