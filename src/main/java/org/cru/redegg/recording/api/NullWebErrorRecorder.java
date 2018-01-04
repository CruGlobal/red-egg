package org.cru.redegg.recording.api;

import com.google.common.collect.Multimap;
import org.joda.time.DateTime;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogRecord;

/**
 * @author Matt Drees
 */
class NullWebErrorRecorder implements WebErrorRecorder
{
    //not a CDI bean
    NullWebErrorRecorder(Object ignored){}

    @Override
    public WebErrorRecorder recordRequestUrl(String url)
    {
        return this;
    }

    @Override
    public WebErrorRecorder recordRequestUrl(URL url)
    {
        return this;
    }

    @Override
    public WebErrorRecorder recordRequestUrl(URI uri)
    {
        return this;
    }

    @Override
    public WebErrorRecorder recordComponent(Method method)
    {
        return this;
    }

    @Override
    public WebErrorRecorder recordRequestStart(DateTime dateTime)
    {
        return this;
    }

    @Override
    public WebErrorRecorder recordHeaders(Multimap<String, String> headers)
    {
        return this;
    }

    @Override
    public WebErrorRecorder recordRequestMethod(String method)
    {
        return this;
    }

    @Override
    public WebErrorRecorder recordRequestQueryParameters(Multimap<String, String> queryParameters)
    {
        return this;
    }

    @Override
    public WebErrorRecorder recordRequestPostParameters(Multimap<String, String> postParameters)
    {
        return this;
    }

    @Override
    public WebErrorRecorder recordEntityRepresentation(String entityRepresentation)
    {
        return this;
    }

    @Override
    public WebErrorRecorder recordResponseStatus(int responseStatus)
    {
        return this;
    }

    @Override
    public WebErrorRecorder recordRequestRemoteIpAddress(String remoteIpAddress)
    {
        return this;
    }

    @Override
    public void startMonitoringRequestForTimeliness()
    {
    }

    @Override
    public void recordRequestComplete(DateTime dateTime)
    {
    }

    @Override
    public ErrorRecorder recordContext(String key, Object object)
    {
        return this;
    }

    @Override
    public ErrorRecorder recordUser(Object user)
    {
        return this;
    }

    @Override
    public ErrorRecorder recordSystemProperties(Properties properties)
    {
        return this;
    }

    @Override
    public ErrorRecorder recordEnvironmentVariables(Map<String, String> variables)
    {
        return this;
    }

    @Override
    public ErrorRecorder recordLocalHost(InetAddress localHost)
    {
        return this;
    }

    @Override
    public ErrorRecorder ignoreLogger(String loggerName)
    {
        return this;
    }

    @Override
    public ErrorRecorder ignoreErrorsFromLogger(String loggerName)
    {
        return this;
    }

    @Override
    public ErrorRecorder userError()
    {
        return this;
    }

    @Override
    public ErrorRecorder mustNotify()
    {
        return this;
    }

    @Override
    public ErrorRecorder recordThrown(Throwable thrown)
    {
        return this;
    }

    @Override
    public ErrorRecorder recordLogRecord(LogRecord record)
    {
        return this;
    }

    @Override
    public void error()
    {
    }

    @Override
    public void sendReportIfNecessary()
    {
    }
}
