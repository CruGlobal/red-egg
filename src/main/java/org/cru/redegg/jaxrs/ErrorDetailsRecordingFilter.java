package org.cru.redegg.jaxrs;

import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.WebErrorRecorder;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Link;

import static org.cru.redegg.Links.CRU_ERROR_DETAILS_REL_TYPE;

/**
 * A {@link ClientResponseFilter} that {@link WebErrorRecorder#recordContext(String, Object) records}
 * the link to the server error details, if such a link was sent back by the server.
 */
public class ErrorDetailsRecordingFilter implements ClientResponseFilter
{

    private final RecorderFactory recorderFactory;

    public ErrorDetailsRecordingFilter(RecorderFactory recorderFactory)
    {
        this.recorderFactory = recorderFactory;
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
    {
        final Link link = responseContext.getLink(CRU_ERROR_DETAILS_REL_TYPE);
        if (link != null)
        {
            final ErrorRecorder recorder = recorderFactory.getRecorder();
            recorder.recordContext("upstream-error-details", link.getUri().toString());
        }
    }
}
