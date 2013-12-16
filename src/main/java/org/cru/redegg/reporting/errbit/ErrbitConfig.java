package org.cru.redegg.reporting.errbit;

import java.net.URL;

/**
 * @author Matt Drees
 */
public class ErrbitConfig
{
    private String key;
    private String environmentName;
    private URL endpoint;

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public URL getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(URL endpoint)
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
