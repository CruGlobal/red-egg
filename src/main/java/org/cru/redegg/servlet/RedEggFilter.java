package org.cru.redegg.servlet;

import org.cru.redegg.recording.api.WebErrorRecorder;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Matt Drees
 */
public class RedEggFilter implements Filter {

    @Inject
    Provider<WebErrorRecorder> errorRecorder;

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
        try {
            chain.doFilter(request, response);
        } catch (IOException e) {
            throw record(request, e);
        } catch (ServletException e) {
            throw record(request, e);
        } catch (RuntimeException e) {
            throw record(request, e);
        } catch (Error e) {
            throw record(request, e);
        }

    }

    private <E extends Throwable> E record(HttpServletRequest request, E e) throws E {
        errorRecorder.get().recordThrown(e);
        throw e;
    }

    @Override
    public void destroy() {
    }
}
