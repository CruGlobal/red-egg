package org.cru.redegg.it;

import org.cru.redegg.manual.Builder;
import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.RedEgg;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * @author Matt Drees
 */
public class ApiWithoutCdiThatErrors extends AbstractApiThatErrors
{
    private RecorderFactory factory = RedEgg.getRecorderFactory();

    @Override
    protected ErrorRecorder recorder()
    {
        return factory.getRecorder();
    }
}
