package org.cru.redegg.recording.api;

import javax.enterprise.context.RequestScoped;
import java.net.InetAddress;
import java.util.Properties;
import java.util.logging.LogRecord;

/**
 * Used to programmatically record an application error
 * or contextual information that would be useful in an error report
 *
 * @author Matt Drees
 */
public interface ErrorRecorder {

    ErrorRecorder recordContext(String key, Object object);

    ErrorRecorder recordUser(Object user);

    /** will not, under any circumstances, throw an exception */
    ErrorRecorder recordThrown(Throwable thrown);

    ErrorRecorder recordLogRecord(LogRecord record);

    ErrorRecorder recordSystemProperties(Properties properties);

    ErrorRecorder recordEnvironmentVariables(Properties properties);

    ErrorRecorder recordLocalHost(InetAddress localHost);

    void error();
}
