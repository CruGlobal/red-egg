package org.cru.redegg.reporting.errbit;

import java.net.URI;
import java.net.URL;

/**
 * @author Matt Drees
 */
public class ErrbitConfig
{
    private String key;
    private String environmentName;
    private URI endpoint;

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public URI getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(URI endpoint)
    {
        this.endpoint = endpoint;
    }

    public String getEnvironmentName()
    {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName)
    {
        this.environmentName = environmentName;
    }
}
