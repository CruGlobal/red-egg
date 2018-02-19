package org.cru.redegg.servlet;

import org.cru.redegg.boot.Initializer;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.util.ErrorLog;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Matt Drees
 */
@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class RedEggFilter implements Filter {

    @Inject
    RecorderFactory factory;

    @Inject
    ErrorLog errorLog;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Initializer.initializeIfNecessary(this, filterConfig.getServletContext());
    }

    @Override
    public void doFilter(
        ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest &&
            response instanceof HttpServletResponse)
        {
            doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
        }
        else {
            chain.doFilter(request, response);
        }
    }

    @SuppressWarnings("TryWithIdenticalCatches") // can't use multicatch; it breaks generic record()
    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        RecordingResponse recordingResponse = new RecordingResponse(response);
        RecordingRequest recordingRequest = new RecordingRequest(request, errorLog, getRecorder());
        try {
            chain.doFilter(recordingRequest, recordingResponse);
        } catch (IOException e) {
            throw record(e);
        } catch (ServletException e) {
            throw record(e);
        } catch (RuntimeException e) {
            throw record(e);
        } catch (Error e) {
            throw record(e);
        }
        finally
        {
            int statusCode = recordingResponse.getStatusCode();
            if (statusCode != -1)
            {
                getRecorder().recordResponseStatus(statusCode);
            }
        }

    }

    private WebErrorRecorder getRecorder()
    {
        try
        {
            return factory.getWebRecorder();
        }
        catch (Throwable throwable)
        {
            errorLog.error("unable to get web recorder", throwable);
            return WebErrorRecorder.NULL_RECORDER;
        }
    }

    private <E extends Throwable> E record(E e) throws E {
        getRecorder().recordThrown(e);
        throw e;
    }

    @Override
    public void destroy() {
    }

    public void setFactory(RecorderFactory factory)
    {
        this.factory = factory;
    }

    public void setErrorLog(ErrorLog errorLog)
    {
        this.errorLog = errorLog;
    }
}
