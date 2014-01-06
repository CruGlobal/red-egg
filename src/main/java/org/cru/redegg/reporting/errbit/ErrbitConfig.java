package org.cru.redegg.reporting.errbit;

import com.google.common.collect.Sets;

import java.net.URI;
import java.util.Set;

/**
 * @author Matt Drees
 */
@Empty
public class ErrbitConfig
{
    private String key;
    private String environmentName;
    private URI endpoint;

    private String sourcePrefix = "src/main/java";
    private Set<String> applicationBasePackages = Sets.newHashSet();

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

    public String getSourcePrefix()
    {
        return sourcePrefix;
    }

    public void setSourcePrefix(String sourcePrefix)
    {
        this.sourcePrefix = sourcePrefix;
    }

    public Set<String> getApplicationBasePackages()
    {
        return applicationBasePackages;
    }

    public void setApplicationBasePackages(Set<String> applicationBasePackages)
    {
        this.applicationBasePackages = applicationBasePackages;
    }
}
