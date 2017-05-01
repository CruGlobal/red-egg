package org.cru.redegg.recording.api;

import com.google.common.collect.Multimap;
import org.joda.time.DateTime;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;

/**
 * @author Matt Drees
 */
public interface WebErrorRecorder extends ErrorRecorder {

    WebErrorRecorder NULL_RECORDER = new NullWebErrorRecorder(null);


    WebErrorRecorder recordRequestUrl(String url);
    WebErrorRecorder recordRequestUrl(URL url);
    WebErrorRecorder recordRequestUrl(URI uri);

    WebErrorRecorder recordRequestQueryString(String queryString);

    /**
     * Indicate that, for this request, the given method is the main 'action',
     * and that the method's containing class is the main 'component'.
     *
     * In other words, if there is one method that, more than any other method,
     * defines what interesting logic happens in this request, that method should be recorded here.
     *
     * This generally will duplicate the information in the request URI and/or post body,
     * but it is often convenient to record this nonetheless, as the mapping rules may not
     * be obvious.
     */
    WebErrorRecorder recordComponent(Method method);

    WebErrorRecorder recordRequestStart(DateTime dateTime);

    WebErrorRecorder recordHeaders(Multimap<String, String> headers);

    WebErrorRecorder recordRequestMethod(String method);

    WebErrorRecorder recordRequestQueryParameters(Multimap<String, String> queryParameters);

    WebErrorRecorder recordRequestPostParameters(Multimap<String, String> postParameters);

    WebErrorRecorder recordEntityRepresentation(String entityRepresentation);

    WebErrorRecorder recordResponseStatus(int responseStatus);

    WebErrorRecorder recordRequestRemoteIpAddress(String remoteIpAddress);

    /**
     * Begin monitoring the request for 'timeliness'.
     * After this point, if the request gets stuck, an a report will be sent.
     *
     * Should be called after several request attributes (path, query parameters, etc) have been recorded,
     * but before the application begins to perform business logic.
     */
    void startMonitoringRequestForTimeliness();

    /**
     * Record the completion of this request.
     *
     * If any errors were recorded during this request, they will now be enqueued for reporting.
     * If the request was monitored for 'timeliness', the request will stop being monitored.
     *
     * @param dateTime when the request completed
     */
    void recordRequestComplete(DateTime dateTime);
}
