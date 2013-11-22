package org.cru.redegg.recording.api;

import javax.enterprise.context.RequestScoped;
import java.util.Properties;
import java.util.logging.LogRecord;

/**
 * Used to programmatically record an application error
 *
 * @author Matt Drees
 */
public interface ErrorRecorder {

    ErrorRecorder recordContext(String key, Object object);

    ErrorRecorder recordUser(Object user);


//    void addThrowable(Throwable thrown);

    /** will not, under any circumstances, throw an exception */
    ErrorRecorder recordError(Throwable message);
//    void recordError(String message, Throwable thrown);

    ErrorRecorder recordLogRecord(LogRecord record);

    ErrorRecorder recordSystemProperties(Properties properties);

    ErrorRecorder recordEnvironmentVariables(Properties properties);


    void error();
}
