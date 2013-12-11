package org.cru.redegg.recording.api;

import com.google.common.collect.Multimap;
import org.joda.time.DateTime;

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

    WebErrorRecorder recordRequestStart(DateTime dateTime);

    WebErrorRecorder recordHeaders(Multimap<String, String> headers);

    WebErrorRecorder recordRequestMethod(String method);

    WebErrorRecorder recordRequestQueryParameters(Multimap<String, String> queryParameters);

    WebErrorRecorder recordRequestPostParameters(Multimap<String, String> postParameters);

    WebErrorRecorder recordEntityRepresentation(String entityRepresentation);

    WebErrorRecorder recordResponseStatus(int responseStatus);


    void recordRequestComplete(DateTime dateTime);
}
