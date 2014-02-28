package org.cru.redegg.manual;

import org.cru.redegg.recording.api.ParameterSanitizer;

import java.util.List;

/**
 * @author Matt Drees
 */
public class ReplaceableParameterSanitizer implements ParameterSanitizer
{

    /* volatile needed because this could be changed by a different thread */
    private volatile ParameterSanitizer delegate;

    public ReplaceableParameterSanitizer(ParameterSanitizer initialDelegate)
    {
        this.delegate = initialDelegate;
    }

    public void replace(ParameterSanitizer newDelegate)
    {
        delegate = newDelegate;
    }

    @Override
    public List<String> sanitizeQueryStringParameter(
        String parameterName,
        List<String> parameterValues)
    {
        return delegate.sanitizeQueryStringParameter(parameterName, parameterValues);
    }

    @Override
    public List<String> sanitizePostBodyParameter(
        String parameterName,
        List<String> parameterValues)
    {
        return delegate.sanitizePostBodyParameter(parameterName, parameterValues);
    }

    @Override
    public List<String> sanitizeHeader(String headerName, List<String> headerValues)
    {
        return delegate.sanitizeHeader(headerName, headerValues);
    }
}
