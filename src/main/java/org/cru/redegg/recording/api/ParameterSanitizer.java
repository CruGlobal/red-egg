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

    /**
     * For generic param sanitization; could be a query string param or post body param
     */
    List<String> sanitizeParameter(String parameterName, List<String> parameterValues);

}
