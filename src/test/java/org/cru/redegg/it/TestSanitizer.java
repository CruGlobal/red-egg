package org.cru.redegg.it;

import com.google.common.collect.ImmutableList;
import org.cru.redegg.recording.api.ParameterSanitizer;

import java.util.List;

/**
* @author Matt Drees
*/
public class TestSanitizer implements ParameterSanitizer
{

    @Override
    public List<String> sanitizeQueryStringParameter(
        String parameterName, List<String> parameterValues)
    {
        return sanitize(parameterName, parameterValues);
    }

    @Override
    public List<String> sanitizePostBodyParameter(
        String parameterName, List<String> parameterValues)
    {
        return sanitize(parameterName, parameterValues);
    }

    private List<String> sanitize(String parameterName, List<String> parameterValues)
    {
        if (parameterName.equals("secret"))
            return ImmutableList.of("<redacted>");
        else
            return parameterValues;
    }
}
