package org.cru.redegg.reporting.errbit;

/**
 * @author Matt Drees
 */
public class ErrbitConfig
{
    private String key;
    private String environmentName;
    private String endpoint;

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(String endpoint)
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
