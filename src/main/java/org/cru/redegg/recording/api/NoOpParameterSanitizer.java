package org.cru.redegg.recording.api;

import javax.enterprise.inject.Alternative;
import java.util.List;

/**
 * Performs no sanitizing at all.  Parameters are returned as-is.
 *
 * To use, activate this alternative in your beans.xml.
 *
 * @author Matt Drees
 */
@Alternative
public class NoOpParameterSanitizer implements ParameterSanitizer
{
    @Override
    public List<String> sanitizeQueryStringParameter(
        String parameterName, List<String> parameterValues)
    {
        return parameterValues;
    }

    @Override
    public List<String> sanitizePostBodyParameter(
        String parameterName, List<String> parameterValues)
    {
        return parameterValues;
    }

    @Override
    public List<String> sanitizeHeader(String headerName, List<String> headerValues)
    {
        return headerValues;
    }

}
