package org.cru.redegg.recording.cdi;

import org.cru.redegg.qualifier.EntityStreamPreservation;
import org.cru.redegg.qualifier.Fallback;
import org.cru.redegg.qualifier.Selected;
import org.cru.redegg.recording.api.RequestMatcher;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

/**
 * @author Matt Drees
 */
public class RequestMatcherProducer
{
    public
    @Produces
    @Selected
    @EntityStreamPreservation
    RequestMatcher selectEntityStreamPreservationRequestMatcher(
        @Default @EntityStreamPreservation Instance<RequestMatcher> defaultMatcher,
        @Fallback @EntityStreamPreservation RequestMatcher fallbackMatcher)
    {
        if (!defaultMatcher.isUnsatisfied())
            return defaultMatcher.get();
        else
            return fallbackMatcher;
    }

}
