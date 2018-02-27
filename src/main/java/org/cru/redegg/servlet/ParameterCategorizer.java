package org.cru.redegg.servlet;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.cru.redegg.qualifier.EntityStreamPreservation;
import org.cru.redegg.qualifier.Selected;
import org.cru.redegg.recording.api.ParameterSanitizer;
import org.cru.redegg.recording.api.RequestMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* @author Matt Drees
*/
public class ParameterCategorizer
{

    private static Logger log = LoggerFactory.getLogger(RedEggServletListener.class);

    private final ParameterSanitizer sanitizer;
    private final RequestMatcher streamPreservationMatcher;

    @Inject
    public ParameterCategorizer(
        @Selected ParameterSanitizer sanitizer,
        @Selected @EntityStreamPreservation RequestMatcher streamPreservationMatcher)
    {
        this.sanitizer = sanitizer;
        this.streamPreservationMatcher = streamPreservationMatcher;
    }

    static class Categorization
    {
        Multimap<String, String> queryParameters;
        Multimap<String, String> postParameters;
        String queryString;

        void sanitizeQueryString(String parameter)
        {
            assert queryString != null : "should only reach this code if queryString is not null";
            String regex = "((^|&)" + Pattern.quote(parameter) + "=)[^&]*";
            Matcher matcher = Pattern.compile(regex).matcher(queryString);

            StringBuffer newQueryString = new StringBuffer(queryString.length());
            Iterator<String> sanitizedValues = queryParameters.get(parameter).iterator();
            String sanitizedValue = null;
            while (matcher.find())
            {
                sanitizedValue = determineSanitizedValue(sanitizedValues, sanitizedValue);
                String replacement = matcher.group(1) + sanitizedValue;
                matcher.appendReplacement(newQueryString, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(newQueryString);
            queryString = newQueryString.toString();
        }

        private String determineSanitizedValue(Iterator<String> sanitizedValues, String sanitizedValue)
        {
            if (sanitizedValues.hasNext())
            {
                sanitizedValue = sanitizedValues.next();
            }
            // otherwise, the sanitizer gave back an unexpected number of values.
            // Just use the last sanitized value that we found,
            // or nothing if there were no sanitized values.

            return Strings.nullToEmpty(sanitizedValue);
        }
    }

    /**
     * Determines which parameters are query string parameters, and which are form parameters.
     * The servlet API doesn't directly give this information.
     */
    Categorization categorize(HttpServletRequest request) {

        if (streamPreservationMatcher.matches(request))
            return empty();

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
        categorization.queryString = request.getQueryString();

        for (String param : keys)
        {
            categorizeParameter(param, request, parameterMap, categorization);
        }
        return categorization;
    }

    private Categorization empty()
    {
        Categorization emptyCategorization = new Categorization();
        emptyCategorization.postParameters = ImmutableMultimap.of();
        emptyCategorization.queryParameters = ImmutableMultimap.of();
        return emptyCategorization;
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
                queryString.equals(param) // eg. /soap/MyServiceEndpoint?wsdl
        );
    }

    private void addQueryStringParameter(
        String parameter,
        Map<String, String[]> parameterMap,
        Categorization categorization)
    {
        List<String> original = Arrays.asList(parameterMap.get(parameter));
        List<String> sanitized = sanitizer.sanitizeQueryStringParameter(parameter, original);
        categorization.queryParameters.putAll(parameter, sanitized);
        if (!original.equals(sanitized))
        {
            categorization.sanitizeQueryString(parameter);
        }
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
            log.warn("parameter not in query string in {} request: {}", request.getMethod(),
                parameter);
        }
    }

}
