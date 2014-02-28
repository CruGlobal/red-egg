package org.cru.redegg.manual;

import com.google.common.collect.ImmutableList;
import org.cru.redegg.recording.api.ParameterSanitizer;

import javax.enterprise.inject.Alternative;
import java.util.List;
import java.util.logging.Logger;

/**
 * Filters out everything
 *
 * @author Matt Drees
 */
@Alternative
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
        warnIfNecesary();
        return ImmutableList.of("<removed>");
    }

    private void warnIfNecesary()
    {
        if (!warned)
        {
            warned = true;
            Logger.getLogger(getClass().getName()).warning(
                "removing all parameter values for this request." +
                " To configure a less conservative strategy use Builder.setParameterSanitizer()");
        }
    }
}
