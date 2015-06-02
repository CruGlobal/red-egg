package org.cru.redegg.recording.api;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.cru.redegg.qualifier.EntityStreamPreservation;
import org.cru.redegg.qualifier.Fallback;

import javax.enterprise.inject.Produces;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * @author Matt Drees
 */
public class RequestMatchers
{
    /**
     * Returns a matcher that matches a request if
     * its servlet path and servlet info path together match one of the given regular expressions.
     *
     * <p>
     * For example, the following will create matcher that matches any jsp requests and
     * any requests to a servlet mapped to {@code /rest-api/*}:
     *
     * <pre> {@code
     *   RequestMatcher.matchingPaths(Arrays.asList(
     *     ".*\.jsp",
     *     "/rest-api/.*");
     * } </pre>
     *
     *
     * @param pathExpressions one or more regular expressions
     */
    public static RequestMatcher matchingPaths(Iterable<String> pathExpressions)
    {
        String combinedExpression = Joiner.on("|").join(pathExpressions);
        return new PathRequestMatcher(Pattern.compile(combinedExpression));
    }

    /**
     * A convenience shortcut for {@link #matchingPaths(Iterable)},
     * for use when the set of expressions can be hardcoded.
     */
    public static RequestMatcher matchingPaths(String... pathExpressions)
    {
        return matchingPaths(Arrays.asList(pathExpressions));
    }

    /**
     * Returns a matcher that will not match any requests.
     */
    @Produces
    @Fallback @EntityStreamPreservation
    public static RequestMatcher none()
    {
        return new RequestMatcher()
        {
            @Override
            public boolean matches(ServletRequest request)
            {
                return false;
            }
        };
    }


    private static class PathRequestMatcher implements RequestMatcher
    {
        private final Pattern urlPattern;

        private PathRequestMatcher(Pattern urlPattern)
        {
            this.urlPattern = urlPattern;
        }

        public boolean matches(ServletRequest request)
        {
            Preconditions.checkNotNull(request, "request is null");
            if (request instanceof HttpServletRequest)
            {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                String fullPath = getFullPath(httpRequest);
                if (urlPattern.matcher(fullPath).matches())
                    return true;
            }
            return false;
        }

        private String getFullPath(HttpServletRequest httpRequest)
        {
            String servletPath = httpRequest.getServletPath();
            servletPath = servletPath == null ? "" : servletPath;
            String pathInfo = httpRequest.getPathInfo();
            pathInfo = pathInfo == null ? "" : pathInfo;
            return servletPath + pathInfo;
        }

    }

}
