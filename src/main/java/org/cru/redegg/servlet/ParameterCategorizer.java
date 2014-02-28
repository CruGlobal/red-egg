package org.cru.redegg.servlet;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.apache.log4j.Logger;
import org.cru.redegg.recording.api.ParameterSanitizer;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* @author Matt Drees
*/
public class ParameterCategorizer
{

    private static Logger log = Logger.getLogger(RedEggServletListener.class);

    private final ParameterSanitizer sanitizer;

    @Inject
    public ParameterCategorizer(ParameterSanitizer sanitizer)
    {
        this.sanitizer = sanitizer;
    }

    static class Categorization
    {
        Multimap<String, String> queryParameters;
        Multimap<String, String> postParameters;
    }

    /**
     * Determines which parameters are query string parameters, and which are form parameters.
     * The servlet API doesn't readily give this information.
     */
    Categorization categorize(HttpServletRequest request) {

        Map<String, String[]> parameterMap = request.getParameterMap();
        Set<String> keys = parameterMap.keySet();

        Categorization categorization = new Categorization();
        if (request.getMethod().equals("POST"))
        {
            categorization.queryParameters = LinkedHashMultimap.create(0, 1);
            categorization.postParameters = LinkedHashMultimap.create(keys.size(), 1);
        }
        else
        {
            categorization.queryParameters = LinkedHashMultimap.create(keys.size(), 1);
            categorization.postParameters = ImmutableMultimap.of();
        }

        for (String param : keys)
        {
            categorizeParameter(param, request, parameterMap, categorization);
        }
        return categorization;
    }

    private void categorizeParameter(
        String parameter,
        HttpServletRequest request,
        Map<String, String[]> parameterMap,
        Categorization categorization)
    {
        String queryString = request.getQueryString();
        if (queryString != null && isQueryParameter(parameter, queryString))
        {
            addQueryStringParameter(parameter, parameterMap, categorization);
        }
        else
        {
            addFormParameter(parameter, request, parameterMap, categorization);
        }
    }

    /* need to verify the param is the 'parameter' part of the query string, and not just a value */
    private boolean isQueryParameter(String param, String queryString) {
        return (queryString.startsWith(param + "=") ||
                queryString.contains('&' + param + "=") ||
                queryString.contains(';' + param + "=") || //believe it or not, ';' is a valid query param separator
                queryString.equals(param) // eg. /soap/MyServiceEndpoint?wsdl
        );
    }

    private void addQueryStringParameter(
        String parameter,
        Map<String, String[]> parameterMap,
        Categorization categorization)
    {
        List<String> sanitized = sanitizer.sanitizeQueryStringParameter(
            parameter,
            Arrays.asList(parameterMap.get(parameter)));
        categorization.queryParameters.putAll(
            parameter,
            sanitized);
    }

    private void addFormParameter(
        String parameter,
        HttpServletRequest request,
        Map<String, String[]> parameterMap,
        Categorization categorization)
    {
        if (request.getMethod().equals("POST")) {
            List<String> sanitized = sanitizer.sanitizePostBodyParameter(
                parameter,
                Arrays.asList(parameterMap.get(parameter)));
            categorization.postParameters.putAll(
                parameter,
                sanitized);
        }
        else
        {
            log.warn(String.format("parameter not in query string in %s request: %s", request.getMethod(),
                parameter));
        }
    }

}
