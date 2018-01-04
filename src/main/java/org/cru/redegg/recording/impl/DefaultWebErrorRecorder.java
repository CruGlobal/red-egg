package org.cru.redegg.recording.impl;

import com.google.common.collect.Multimap;
import org.cru.redegg.qualifier.Selected;
import org.cru.redegg.recording.StuckThreadMonitor;
import org.cru.redegg.recording.api.EntitySanitizer;
import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.cru.redegg.reporting.api.ErrorQueue;
import org.cru.redegg.util.ErrorLog;
import org.cru.redegg.util.ProxyConstructor;
import org.joda.time.DateTime;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogRecord;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.cru.redegg.recording.api.NotificationLevel.ERROR;

/**
 * @author Matt Drees
 */
@RequestScoped
public class DefaultWebErrorRecorder implements WebErrorRecorder {

    private final DefaultErrorRecorder defaultRecorder;

    private final ErrorQueue queue;

    private final ErrorLog errorLog;
    private final EntitySanitizer entitySanitizer;
    private final StuckThreadMonitor stuckThreadMonitor;

    @Inject
    public DefaultWebErrorRecorder(
        DefaultErrorRecorder defaultRecorder,
        ErrorQueue queue,
        ErrorLog errorLog,
        @Selected EntitySanitizer entitySanitizer,
        StuckThreadMonitor stuckThreadMonitor)
    {
        this.defaultRecorder = defaultRecorder;
        this.queue = queue;
        this.errorLog = errorLog;
        this.entitySanitizer = entitySanitizer;
        this.stuckThreadMonitor = stuckThreadMonitor;
    }

    @ProxyConstructor
    protected DefaultWebErrorRecorder(){
        defaultRecorder = null;
        queue = null;
        errorLog = null;
        entitySanitizer = null;
        stuckThreadMonitor = null;
    }

    private final WebContext webContext = new WebContext();

    private boolean completed;

    @Override
    public WebErrorRecorder recordRequestUrl(String url) {
        checkState(!completed);
        try {
            webContext.setUrl(new URI(url));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public WebErrorRecorder recordRequestUrl(URL url) {
        checkState(!completed);
        try {
            webContext.setUrl(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public WebErrorRecorder recordRequestUrl(URI uri) {
        checkState(!completed);
        checkNotNull(uri);
        webContext.setUrl(uri);
        return this;
    }

    @Override
    public WebErrorRecorder recordComponent(Method method)
    {
        checkState(!completed);
        checkNotNull(method);
        webContext.setComponent(method);
        return this;
    }

    @Override
    public WebErrorRecorder recordRequestStart(DateTime start) {
        checkState(!completed);
        webContext.setStart(start);
        return this;
    }

    @Override
    public WebErrorRecorder recordHeaders(Multimap<String, String> headers) {
        checkState(!completed);
        webContext.setHeaders(checkNotNull(headers));
        return this;
    }

    @Override
    public WebErrorRecorder recordRequestMethod(String method) {
        checkState(!completed);
        webContext.setMethod(checkNotNull(method));
        return this;
    }

    @Override
    public WebErrorRecorder recordRequestQueryParameters(Multimap<String, String> queryParameters) {
        checkState(!completed);
        webContext.setQueryParameters(checkNotNull(queryParameters));
        return this;
    }

    @Override
    public WebErrorRecorder recordRequestPostParameters(Multimap<String, String> postParameters) {
        checkState(!completed);
        webContext.setPostParameters(checkNotNull(postParameters));
        return this;
    }

    @Override
    public WebErrorRecorder recordEntityRepresentation(String entityRepresentation) {
        checkState(!completed);
        checkNotNull(entityRepresentation);
        String sanitized = entitySanitizer.sanitizeEntity(entityRepresentation);
        webContext.setEntityRepresentation(sanitized);
        return this;
    }

    @Override
    public WebErrorRecorder recordResponseStatus(int responseStatus)
    {
        checkState(!completed);
        webContext.setResponseStatus(responseStatus);
        return this;
    }

    @Override
    public WebErrorRecorder recordRequestRemoteIpAddress(String remoteIpAddress)
    {
        checkState(!completed);
        webContext.setRemoteIpAddress(remoteIpAddress);
        return this;
    }

    @Override
    public void recordRequestComplete(DateTime finish) {
        checkState(!completed);
        completed = true;

        stuckThreadMonitor.finishMonitoringRequest(webContext);

        webContext.setFinish(finish);
        if (defaultRecorder.shouldNotificationBeSent())
        {
            if (wasClientError())
            {
                defaultRecorder.userError();
            }
            defaultRecorder.addAdditionalContextIfPossible();
            ErrorReport report = defaultRecorder.buildReport();
            report.addWebContext(webContext);
            queue.enqueue(report);
        }
    }

    private boolean wasClientError()
    {
        Integer status = webContext.getResponseStatus();
        return
            status != null &&
            status >= 400 &&
            status < 500;
    }


    @Override
    public ErrorRecorder recordContext(String key, Object object) {
        checkState(!completed);
        defaultRecorder.recordContext(key, object);
        return this;
    }

    @Override
    public ErrorRecorder recordUser(Object user) {
        checkState(!completed);
        defaultRecorder.recordUser(user);
        return this;
    }

    @Override
    public ErrorRecorder recordThrown(Throwable thrown) {
        try
        {
            checkState(!completed);
            defaultRecorder.recordThrown(thrown);
        }
        catch (Throwable t)
        {
            errorLog.error("unable to record thrown exception due to the following secondary exception; " +
                      "logging and swallowing instead to avoid masking original", t);
        }
        return this;
    }

    @Override
    public ErrorRecorder recordLogRecord(LogRecord record) {
        checkState(!completed);
        defaultRecorder.recordLogRecord(record);
        return this;
    }

    @Override
    public ErrorRecorder recordSystemProperties(Properties properties) {
        checkState(!completed);
        defaultRecorder.recordSystemProperties(properties);
        return this;
    }

    @Override
    public ErrorRecorder recordEnvironmentVariables(Map<String, String> variables) {
        checkState(!completed);
        defaultRecorder.recordEnvironmentVariables(variables);
        return this;
    }

    @Override
    public ErrorRecorder recordLocalHost(InetAddress localHost) {
        checkState(!completed);
        defaultRecorder.recordLocalHost(localHost);
        return this;
    }

    @Override
    public ErrorRecorder ignoreLogger(String loggerName)
    {
        checkState(!completed);
        defaultRecorder.ignoreLogger(loggerName);
        return this;
    }

    @Override
    public ErrorRecorder ignoreErrorsFromLogger(String loggerName)
    {
        checkState(!completed);
        defaultRecorder.ignoreErrorsFromLogger(loggerName);
        return this;
    }

    @Override
    public ErrorRecorder mustNotify()
    {
        checkState(!completed);
        defaultRecorder.mustNotify();
        return this;
    }

    @Override
    public void error() {
        checkState(!completed);
        defaultRecorder.ensureNotificationLevel(ERROR);
    }

    @Override
    public void sendReportIfNecessary()
    {
    }

    @Override
    public ErrorRecorder userError()
    {
        checkState(!completed);
        defaultRecorder.userError();
        return this;
    }

    @Override
    public void startMonitoringRequestForTimeliness()
    {
        stuckThreadMonitor.startMonitoringRequest(webContext);
    }

    @Override
    public void suspendRequestProcessing()
    {
        stuckThreadMonitor.finishMonitoringRequest(webContext);
    }
}
