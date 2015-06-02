package org.cru.redegg.manual;

import org.cru.redegg.recording.api.RequestMatcher;

import javax.servlet.ServletRequest;

/**
 * @author Matt Drees
 */
public class ReplaceableRequestMatcher implements RequestMatcher
{
    private volatile RequestMatcher delegate;

    public ReplaceableRequestMatcher(RequestMatcher initialDelegate)
    {
        this.delegate = initialDelegate;
    }


    public void replace(RequestMatcher newDelegate)
    {
        delegate = newDelegate;
    }

    @Override
    public boolean matches(ServletRequest request)
    {
        return delegate.matches(request);
    }
}
