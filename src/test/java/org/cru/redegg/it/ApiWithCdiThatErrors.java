package org.cru.redegg.it;

import org.apache.log4j.Logger;
import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.WebErrorRecorder;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * @author Matt Drees
 */
public class ApiWithCdiThatErrors extends AbstractApiThatErrors
{
    @Inject
    //TODO: set up dependencies such that user can just inject ErrorRecorder
    WebErrorRecorder recorder;

    @Override
    protected ErrorRecorder recorder()
    {
        return recorder;
    }
}
