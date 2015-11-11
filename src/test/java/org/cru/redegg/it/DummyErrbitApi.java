package org.cru.redegg.it;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
* @author Matt Drees
*/
@Path("/dummyapi/notices")
public class DummyErrbitApi
{
    volatile static String report;

    @POST
    public synchronized void postNotice(String xmlPayload)
    {
        report = xmlPayload;
    }

    @GET
    public synchronized String getMostRecentReport()
    {
        String returnedReport = report;
        report = null;
        return returnedReport;
    }

}
