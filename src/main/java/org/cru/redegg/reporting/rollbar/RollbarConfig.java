package org.cru.redegg.reporting.rollbar;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Matt Drees
 */
public class RollbarConfig
{
    public static final String ROLLBAR_DEFAULT_ENDPOINT = "https://api.rollbar.com/api/1/item/";

    private String accessToken;
    private URI endpoint;
    private String environmentName;
    private String codeVersion;
    private String branch;

    //TODO: is this helful?
//    private String root;

    private String platform = "java ee";
    private String identifyingUserProperty = "id";


    public RollbarConfig()
    {
        try
        {
            endpoint = new URI(ROLLBAR_DEFAULT_ENDPOINT);
        }
        catch (URISyntaxException e)
        {
            throw new AssertionError(e);
        }
    }

    public String getAccessToken()
    {
        return accessToken;
    }

    public void setAccessToken(String accessToken)
    {
        this.accessToken = accessToken;
    }

    public String getEnvironmentName()
    {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName)
    {
        this.environmentName = environmentName;
    }

    public URI getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(URI endpoint)
    {
        this.endpoint = endpoint;
    }

    public String getCodeVersion()
    {
        return codeVersion;
    }

    /**
     *  Sets the code version for this application.
     *  The code version is a string, up to 40 characters,
     *  describing the version of the application code.
     *  Rollbar understands these formats:
     *   -  semantic version (i.e. "2.1.12")
     *   -  integer (i.e. "45")
     *   -  git SHA (i.e. "3da541559918a808c2402bba5012f6c60b27661c")
     */
    public void setCodeVersion(String codeVersion)
    {
        this.codeVersion = codeVersion;
    }

    public String getPlatform()
    {
        return platform;
    }

    public void setPlatform(String platform)
    {
        this.platform = platform;
    }

    public String getIdentifyingUserProperty()
    {
        return identifyingUserProperty;
    }

    /**
     * Sets the user property name that uniquely identifies a user in this application.
     *
     * It defaults to 'id'.
     *
     * Rollbar tracks users by this identifier.
     *
     * TODO: finish docs here
     */
    public void setIdentifyingUserProperty(String identifyingUserProperty)
    {
        this.identifyingUserProperty = identifyingUserProperty;
    }

    public String getBranch()
    {
        return branch;
    }

    /** Sets the name of the checked-out source control branch. Defaults to "master". */
    public void setBranch(String branch)
    {
        this.branch = branch;
    }
}
