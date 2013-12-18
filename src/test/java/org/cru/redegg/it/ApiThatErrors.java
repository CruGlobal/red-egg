package org.cru.redegg.it;

import org.apache.log4j.Logger;
import org.cru.redegg.recording.api.WebErrorRecorder;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
* @author Matt Drees
*/
@Path("/explosions")
public class ApiThatErrors
{

    @Inject
    //TODO: set up dependencies such that user can just inject ErrorRecorder
        WebErrorRecorder recorder;

    @POST
    @Path("throw")
    public void throwBoom()
    {
        recorder.recordUser(new User(42, "joe.staffguy@cru.org"));
        recorder.recordContext("fun fact:", "I'm about to blow");
        throw new IllegalStateException("kablooie!");
    }

    @POST
    @Path("log")
    public void logBoom()
    {
        recorder.recordUser(new User(42, "joe.staffguy@cru.org"));
        recorder.recordContext("fun fact:", "I'm about to blow");
        Logger.getLogger(getClass()).error("kablooie!");
    }

    public static class User
    {
        int id;
        String name;

        public User(int id, String name)
        {
            this.id = id;
            this.name = name;
        }
    }
}
