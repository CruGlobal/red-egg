package org.cru.redegg.servlet;

import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.util.ErrorLog;

import javax.inject.Inject;
import javax.inject.Provider;
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
@WebFilter(urlPatterns = "/*")
public class RedEggFilter implements Filter {

    @Inject
    Provider<WebErrorRecorder> errorRecorder;

    @Inject
    ErrorLog errorLog;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
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
            return errorRecorder.get();
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
}
