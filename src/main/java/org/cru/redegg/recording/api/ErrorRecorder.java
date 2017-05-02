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

    /**
     * Indicates that no messages from the given logger should be recorded.
     * (Perhaps they are too big, are not useful, or contain sensitive info)
     */
    ErrorRecorder ignoreLogger(String loggerName);

    /**
     * Indicates that messages from the given logger should not trigger notifications,
     * even if the level is WARN or ERROR.
     *
     * (Perhaps the messages are faulty for some reason, and should be suppressed.)
     */
    ErrorRecorder ignoreErrorsFromLogger(String loggerName);


    /**
     * indicates that this the error recorded (if any) was definitely the user/client's fault.
     *
     * This is normally inferred by the HTTP status code, but sometimes a 500 has to be
     * used even if the error is the client's.
     *
     * (For example, with SOAP).
     *
     */
    ErrorRecorder userError();

    //these three may mark the current request (if any) as an error request

    /**
     * Indicates that an error report must be sent,
     * even if it is a user error.
     */
    ErrorRecorder mustNotify();

    /** will not, under any circumstances, throw an exception */
    ErrorRecorder recordThrown(Throwable thrown);

    ErrorRecorder recordLogRecord(LogRecord record);



    /**
     * May trigger an error report immediately,
     * or may flag the current request as needing an error report when it completes
     * */
    void error();

    /**
     * triggers an error report if any errors were added and this is not
     * a web error recorder.
     * (For those, errors are always sent at the end of the request)
     */
    void sendReportIfNecessary();
}
