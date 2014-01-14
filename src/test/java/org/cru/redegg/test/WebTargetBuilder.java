package org.cru.redegg.test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.net.URISyntaxException;
import java.net.URL;

public class WebTargetBuilder
{
    public WebTarget getWebTarget(String context)
    {
        int port = Integer.parseInt(System.getProperty("jboss.http.port", "8080"));
        return ClientBuilder.newClient().target("http://localhost:" + port + "/" + context + "/" + TestApplication.REST_PATH);
    }

    public WebTarget getWebTarget(URL deploymentURL)
    {
        try
        {
            return ClientBuilder.newClient()
                .target(deploymentURL.toURI())
                .path(TestApplication.REST_PATH);
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }
}
