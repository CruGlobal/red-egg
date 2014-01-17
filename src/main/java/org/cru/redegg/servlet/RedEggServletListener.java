package org.cru.redegg.servlet;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.cru.redegg.Lifecycle;
import org.cru.redegg.recording.api.ParameterSanitizer;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.util.Clock;

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
    ParameterSanitizer sanitizer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        lifecycle.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        lifecycle.stop();
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
        WebErrorRecorder recorder = recorderFactory.getWebRecorder()
            // capture the current time as early as possible
            .recordRequestStart(clock.dateTime())
            .recordRequestUrl(request.getRequestURL().toString())
            .recordRequestMethod(request.getMethod())
            .recordHeaders(getHeadersAsMultimap(request));

        Categorization categorization = categorizer.categorize(request);

        recorder
            .recordRequestQueryParameters(categorization.queryParameters)
            .recordRequestPostParameters(categorization.postParameters);
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
        sre.getServletRequest();

        recorderFactory.getWebRecorder()
            .recordRequestComplete(clock.dateTime());
    }


}
