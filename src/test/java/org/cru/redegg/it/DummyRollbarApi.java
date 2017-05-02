package org.cru.redegg.it;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

/**
* @author Matt Drees
*/
@Path("/dummyapi/notices")
public class DummyRollbarApi
{
    volatile static String report;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public synchronized SuccessResponse postNotice(String jsonPayload)
    {
        report = jsonPayload;
        return new SuccessResponse();
    }

    @GET
    public synchronized String getMostRecentReport()
    {
        String returnedReport = report;
        report = null;
        return returnedReport;
    }

    public static class SuccessResponse
    {
        private String uuid = UUID.randomUUID().toString();

        public String getUuid()
        {
            return uuid;
        }

        public void setUuid(String uuid)
        {
            this.uuid = uuid;
        }
    }

}
