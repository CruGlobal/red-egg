package org.cru.redegg.servlet;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.cru.redegg.boot.Initializer;
import org.cru.redegg.boot.Lifecycle;
import org.cru.redegg.qualifier.Selected;
import org.cru.redegg.recording.api.ParameterSanitizer;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.util.Clock;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.cru.redegg.servlet.ParameterCategorizer.Categorization;

/**
 * @author Matt Drees
 */
@WebListener
public class RedEggServletListener implements ServletContextListener, ServletRequestListener {

    @Inject
    RecorderFactory recorderFactory;

    @Inject
    Clock clock;

    @Inject
    ParameterCategorizer categorizer;

    @Inject
    Lifecycle lifecycle;

    @Inject
    @Selected
    ParameterSanitizer sanitizer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Initializer.initializeIfNecessary(this, sce.getServletContext());
        lifecycle.beginApplication();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        lifecycle.endApplication();
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        ServletRequest request = sre.getServletRequest();
        if (request instanceof HttpServletRequest) {
            requestInitialized((HttpServletRequest) request);
        }
    }

    private void requestInitialized(HttpServletRequest request)
    {
        // if we are in an async contextual state, then the request has already been initialized
        if(isAsyncContext(request)) {
            return;
        }

        // capture the current time as early as possible
        DateTime requestStart = clock.dateTime();

        lifecycle.beginRequest();
        WebErrorRecorder recorder = recorderFactory.getWebRecorder()
            .recordRequestStart(requestStart)
            .recordRequestRemoteIpAddress(request.getRemoteAddr())
            .recordRequestUrl(request.getRequestURL().toString())
            .recordRequestMethod(request.getMethod())
            .recordHeaders(getHeadersAsMultimap(request));

        Categorization categorization = categorizer.categorize(request);

        recorder
            .recordRequestQueryParameters(categorization.queryParameters)
            .recordRequestPostParameters(categorization.postParameters)
            .startMonitoringRequestForTimeliness();
    }

    private Multimap<String, String> getHeadersAsMultimap(HttpServletRequest request) {

        Multimap<String, String> httpHeaders = LinkedHashMultimap.create(8, 1);

        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames == null)
            return httpHeaders;

        while (headerNames.hasMoreElements())
        {
            String headerName = headerNames.nextElement();
            List<String> rawValues = Collections.list(request.getHeaders(headerName));
            List<String> sanitizedValues = sanitizer.sanitizeHeader(headerName, rawValues);
            httpHeaders.putAll(headerName, sanitizedValues);
        }

        return httpHeaders;
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        final WebErrorRecorder recorder = recorderFactory.getWebRecorder();
        // if we are in an async contextual state, then the request has not yet completed
        if(isAsyncStarted(sre)) {
            setAsyncContext(sre.getServletRequest(), true);
            recorder.suspendRequestProcessing();
        } else {
            recorder.recordRequestComplete(clock.dateTime());
            lifecycle.endRequest();
            setAsyncContext(sre.getServletRequest(), false);
        }
    }

    private boolean isAsyncStarted(ServletRequestEvent event)
    {
        return event.getServletContext().getMajorVersion() >= 3 &&
               event.getServletRequest().isAsyncStarted();
    }

    public void setRecorderFactory(RecorderFactory recorderFactory)
    {
        this.recorderFactory = recorderFactory;
    }

    public void setClock(Clock clock)
    {
        this.clock = clock;
    }

    public void setCategorizer(ParameterCategorizer categorizer)
    {
        this.categorizer = categorizer;
    }

    public void setLifecycle(Lifecycle lifecycle)
    {
        this.lifecycle = lifecycle;
    }

    public void setSanitizer(ParameterSanitizer sanitizer)
    {
        this.sanitizer = sanitizer;
    }

    /**
     * Async context state management
     */
    private final String ASYNC_CONTEXT = getClass().getName() + ".asyncContext";

    private boolean isAsyncContext(ServletRequest request) {
        return request.getAttribute(ASYNC_CONTEXT) != null &&
                request.getAttribute(ASYNC_CONTEXT).equals(Boolean.toString(true));
    }

    private void setAsyncContext(ServletRequest request, boolean asyncContext) {
        request.setAttribute(ASYNC_CONTEXT, Boolean.toString(asyncContext));
    }
}
