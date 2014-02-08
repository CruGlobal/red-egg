package org.cru.redegg.it;

import org.cru.redegg.reporting.errbit.ErrbitConfig;
import org.cru.redegg.test.WebTargetBuilder;

import javax.enterprise.inject.Produces;
import java.net.URISyntaxException;

/**
* @author Matt Drees
*/
public class ConfigProducer
{
    @Produces
    public ErrbitConfig buildConfig() throws URISyntaxException
    {
        ErrbitConfig config = new ErrbitConfig();
        config.setEndpoint(new WebTargetBuilder().getWebTarget("end-to-end-test").path("/dummyapi/notices").getUri());
        config.setKey("abc");
        config.setEnvironmentName("integration-testing");
        config.getApplicationBasePackages().add("org.cru.redegg");
        config.setSourcePrefix("src/test/java");
        return config;
    }
}
