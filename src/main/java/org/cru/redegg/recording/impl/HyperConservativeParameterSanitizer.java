package org.cru.redegg.recording.impl;

import com.google.common.collect.ImmutableList;
import org.cru.redegg.recording.api.ParameterSanitizer;
import org.cru.redegg.qualifier.Fallback;

import java.util.List;
import java.util.logging.Logger;

/**
 * A parameter sanitizer that filters out everything.
 *
 * This is the parameter sanitizer used if the user does not configure one.
 *
 * @author Matt Drees
 */
@Fallback
public class HyperConservativeParameterSanitizer implements ParameterSanitizer
{
    private volatile static boolean warned = false;

    @Override
    public List<String> sanitizeQueryStringParameter(
        String parameterName, List<String> parameterValues)
    {
        return sanitize();
    }

    @Override
    public List<String> sanitizePostBodyParameter(
        String parameterName, List<String> parameterValues)
    {
        return sanitize();
    }

    @Override
    public List<String> sanitizeHeader(String headerName, List<String> headerValues)
    {
        return sanitize();
    }

    private List<String> sanitize()
    {
        warnIfNecessary();
        return ImmutableList.of("<removed>");
    }

    private void warnIfNecessary()
    {
        if (!warned)
        {
            warned = true;
            Logger.getLogger(getClass().getName()).warning(
                "removing all parameter values for this request." +
                " To avoid this message or to gather more detail," +
                " configure a less conservative strategy");
        }
    }
}
