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

    public static final WebErrorRecorder NULL_RECORDER = new NullWebErrorRecorder(null);


    WebErrorRecorder recordRequestUrl(String url);
    WebErrorRecorder recordRequestUrl(URL url);
    WebErrorRecorder recordRequestUrl(URI uri);

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

    void recordRequestComplete(DateTime dateTime);
}
