package org.cru.redegg.servlet;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.apache.log4j.Logger;
import org.cru.redegg.recording.api.ParameterSanitizer;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
* @author Matt Drees
*/
public class ParameterSorter
{

    private static Logger log = Logger.getLogger(RedEggServletListener.class);

    @Inject
    ParameterSanitizer sanitizer;


    static class Sort
    {
        Multimap<String, String> queryParameters;
        Multimap<String, String> postParameters;
    }

    Sort sort(HttpServletRequest request) {

        //HttpServletRequest does not provide generic API
        @SuppressWarnings("unchecked")
        Map<String, String[]> parameterMap = request.getParameterMap();
        Set<String> keys = parameterMap.keySet();

        Sort sort = new Sort();
        if (request.getMethod().equals("POST"))
        {
            sort.queryParameters = LinkedHashMultimap.create(0, 1);
            sort.postParameters = LinkedHashMultimap.create(keys.size(), 1);
        }
        else
        {
            sort.queryParameters = LinkedHashMultimap.create(keys.size(), 1);
            sort.postParameters = ImmutableMultimap.of();
        }

        for (String param : keys)
        {
            String queryString = request.getQueryString();
            if (queryString != null && isQueryParameter(param, queryString))
            {
                sort.queryParameters.putAll(
                    param,
                    sanitizer.sanitizeQueryStringParameter(
                        param,
                        Arrays.asList(parameterMap.get(param))));
            }
            else
            {
                if (request.getMethod().equals("POST")) {
                    sort.postParameters.putAll(
                        param,
                        sanitizer.sanitizePostBodyParameter(
                            param,
                            Arrays.asList(parameterMap.get(param))));
                } else
                {
                    log.warn(String.format("parameter not in query string in %s request: %s", request.getMethod(), param));
                }
            }
        }
        return sort;
    }

    /* need to verify the param is the 'parameter' part of the query string, and not just a value */
    private boolean isQueryParameter(String param, String queryString) {
        return (queryString.startsWith(param + "=") ||
                queryString.contains('&' + param + "=") ||
                queryString.contains(';' + param + "=") || //believe it or not, ';' is a valid query param separator
                queryString.equals(param) // eg. /soap/MyServiceEndpoint?wsdl
        );
    }

}
