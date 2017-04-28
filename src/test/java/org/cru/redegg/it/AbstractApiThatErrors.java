package org.cru.redegg.it;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.cru.redegg.recording.api.ErrorRecorder;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

/**
* @author Matt Drees
*/
@Path("/explosions")
public abstract class AbstractApiThatErrors
{

    @POST
    @Path("throw")
    public void throwBoom()
    {
        Logger logger = Logger.getLogger(getClass());
        logger.debug("joe is logged in");
        recordUser();
        recorder().recordContext("fun fact:", "I'm about to blow");
        throw new IllegalStateException(createBoom());
    }

    private BoomException createBoom()
    {
        return new BoomException(500, "kablooie!");
    }

    @POST
    @Path("throw")
    @Consumes(MediaType.APPLICATION_JSON)
    /* we don't use the payload, but we need to make sure jax-rs reads it for the test */
    public void throwBoom(String payload)
    {
        this.throwBoom();
    }


    @POST
    @Path("log")
    public void logBoom()
    {
        recordUser();
        recorder().recordContext("fun fact:", "I'm about to blow");
        MDC.put("purpose", "error testing");
        NDC.push("level 0");

        Logger logger = Logger.getLogger(getClass());
        logger.info("minding my own business when...");
        logger.error("kablooie!");
        NDC.pop();
    }

    private void recordUser()
    {
        recorder().recordUser(new User(
            42,
            "joe.staffguy@cru.org",
            "joe.staffguy@gmail.com",
            "12345678-abcd-abcd-abcd-1234567890ab"));
    }

    protected abstract ErrorRecorder recorder();

    public static class User
    {
        final int id;
        final String username;
        final String email;
        final String relayGuid;

        public User(
            int id,
            String username,
            String email,
            String relayGuid)
        {
            this.id = id;
            this.username = username;
            this.email = email;
            this.relayGuid = relayGuid;
        }
    }

    public static class BoomException extends Exception
    {
        private final int boomCode;

        public BoomException(int boomCode, String message)
        {
            super(message);
            this.boomCode = boomCode;
        }

        @SuppressWarnings("unused")
        public int getBoomCode()
        {
            return boomCode;
        }
    }
}
