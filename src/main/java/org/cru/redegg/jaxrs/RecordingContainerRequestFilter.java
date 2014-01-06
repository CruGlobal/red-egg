package org.cru.redegg.jaxrs;

import org.cru.redegg.recording.api.WebErrorRecorder;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * @author Matt Drees
 */
@Provider
public class RecordingContainerRequestFilter implements ContainerRequestFilter
{

    @Context
    ResourceInfo resourceInfo;

    @Inject
    WebErrorRecorder recorder;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException
    {
        recorder.recordComponent(resourceInfo.getResourceMethod());
    }
}
