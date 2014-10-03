package org.cru.redegg.test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class PortBuilder
{
    public <T> T getPort(Class<T> type, QName serviceName, String context, String wsdlPath)
    {
        int port = Integer.parseInt(System.getProperty("jboss.http.port", "8080"));
        String wsdlUrl = "http://localhost:" + port + "/" + context + "/" + wsdlPath;

        Service service;
        try
        {
            service = Service.create(new URL(wsdlUrl), serviceName);
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }

        return service.getPort(type);
    }

}
