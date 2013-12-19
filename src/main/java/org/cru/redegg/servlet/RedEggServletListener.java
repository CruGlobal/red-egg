package org.cru.redegg.servlet;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.cru.redegg.Lifecycle;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.util.Clock;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

import static org.cru.redegg.servlet.ParameterCategorizer.Categorization;

/**
 * @author Matt Drees
 */
@WebListener
public class RedEggServletListener implements ServletContextListener, ServletRequestListener {


    @Inject
    RecorderFactory recorderFactory;

    @Inject
    Provider<WebErrorRecorder> errorRecorder;

    @Inject
    Clock clock;

    @Inject
    ParameterCategorizer categorizer;

    @Inject
    Lifecycle lifecycle;

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
        WebErrorRecorder recorder = errorRecorder.get()
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

        //HttpServletRequest does not provide a generic API
        @SuppressWarnings("unchecked")
        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames == null)
            return httpHeaders;

        while (headerNames.hasMoreElements())
        {
            String headerName = headerNames.nextElement();
            @SuppressWarnings("unchecked")
            Enumeration<String> values = request.getHeaders(headerName);
            while (values.hasMoreElements())
            {
                httpHeaders.put(headerName, values.nextElement());
            }
        }

        return httpHeaders;
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        sre.getServletRequest();

        errorRecorder.get()
            .recordRequestComplete(clock.dateTime());
    }


}
