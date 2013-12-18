package org.cru.redegg.recording.api;

import java.util.List;


public interface ParameterSanitizer
{

    /**
     * Sanitize a query string (aka 'GET') parameter
     */
    List<String> sanitizeQueryStringParameter(String parameterName, List<String> parameterValues);

    /**
     * Sanitize a post body parameter
     */
    List<String> sanitizePostBodyParameter(String parameterName, List<String> parameterValues);

}
