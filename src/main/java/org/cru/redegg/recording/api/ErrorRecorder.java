package org.cru.redegg.recording.api;

import java.net.InetAddress;
import java.util.Map;
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

    ErrorRecorder recordSystemProperties(Properties properties);

    ErrorRecorder recordEnvironmentVariables(Map<String, String> variables);

    ErrorRecorder recordLocalHost(InetAddress localHost);

//    ErrorRecorder recordEnvironmentName(String environmentName);


    //these two may mark the current request (if any) as an error request

    /** will not, under any circumstances, throw an exception */
    ErrorRecorder recordThrown(Throwable thrown);

    ErrorRecorder recordLogRecord(LogRecord record);

    /**
     * May trigger an error report immediately,
     * or may flag the current request as needing an error report when it completes
     * */
    void error();
}
