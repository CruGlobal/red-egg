package org.cru.redegg.recording.cdi;

import org.cru.redegg.qualifier.Fallback;
import org.cru.redegg.qualifier.Selected;
import org.cru.redegg.recording.api.EntitySanitizer;
import org.cru.redegg.recording.api.ParameterSanitizer;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

/**
 * @author Matt Drees
 */
public class SanitizerProducer
{

    public
    @Produces
    @Selected ParameterSanitizer selectParameterSanitizer(
        @Default Instance<ParameterSanitizer> defaultSanitizer,
        @Fallback ParameterSanitizer fallbackSanitizer)
    {
        if (!defaultSanitizer.isUnsatisfied())
            return defaultSanitizer.get();
        else
            return fallbackSanitizer;
    }

    public
    @Produces
    @Selected EntitySanitizer selectEntitySanitizer(
        @Default Instance<EntitySanitizer> defaultSanitizer,
        @Fallback EntitySanitizer fallbackSanitizer)
    {
        if (!defaultSanitizer.isUnsatisfied())
            return defaultSanitizer.get();
        else
            return fallbackSanitizer;
    }

}
