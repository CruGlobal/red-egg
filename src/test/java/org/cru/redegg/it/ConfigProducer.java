package org.cru.redegg.it;

import org.cru.redegg.reporting.rollbar.RollbarConfig;
import org.cru.redegg.test.WebTargetBuilder;

import javax.enterprise.inject.Produces;

/**
* @author Matt Drees
*/
public class ConfigProducer
{
    @Produces
    public RollbarConfig buildConfig()
    {
        RollbarConfig config = new RollbarConfig();
        config.setEndpoint(new WebTargetBuilder().getWebTarget("end-to-end-test").path("/dummyapi/notices").getUri());
        config.setAccessToken("abc");
        config.setEnvironmentName("integration-testing");
        return config;
    }
}
