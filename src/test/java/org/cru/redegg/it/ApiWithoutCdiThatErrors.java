package org.cru.redegg.it;

import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.RedEgg;

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
