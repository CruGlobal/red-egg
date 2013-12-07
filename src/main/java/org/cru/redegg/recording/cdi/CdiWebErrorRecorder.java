package org.cru.redegg.recording.cdi;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import org.cru.redegg.reporting.ErrorQueue;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.recording.impl.DefaultErrorRecorder;
import org.cru.redegg.util.Log;
import org.joda.time.DateTime;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogRecord;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Matt Drees
 */
@RequestScoped
public class CdiWebErrorRecorder implements WebErrorRecorder {

    DefaultErrorRecorder defaultRecorder;

    WebContext webContext;

    @Inject
    Log log;

    @Inject
    ErrorQueue queue;

    private boolean error;
    private boolean completed;

    @PostConstruct
    public void init()
    {
        defaultRecorder = new DefaultErrorRecorder(queue);
    }

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
    public void recordRequestComplete(DateTime finish) {
        checkState(!completed);
        completed = true;
        webContext.setFinish(finish);
        if (error || defaultRecorder.wereErrorsAdded())
        {
            ErrorReport report = defaultRecorder.buildReport();
            report.addWebContext(webContext);
            queue.enqueue(report);
        }
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
            log.error("unable to record thrown exception due to the following secondary exception; " +
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
    public void error() {
        checkState(!completed);
        error = true;
    }
}
