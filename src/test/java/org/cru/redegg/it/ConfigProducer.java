package org.cru.redegg.it;

import java.time.Duration;
import org.cru.redegg.recording.StuckThreadMonitorConfig;
import org.cru.redegg.reporting.rollbar.RollbarConfig;
import org.cru.redegg.test.WebTargetBuilder;

import javax.enterprise.inject.Produces;
import java.util.concurrent.TimeUnit;

/**
* @author Matt Drees
*/
public class ConfigProducer
{
    @Produces
    public RollbarConfig buildRollbarConfig()
    {
        RollbarConfig config = new RollbarConfig();
        config.setEndpoint(new WebTargetBuilder().getWebTarget("end-to-end-test").path("/dummyapi/notices").getUri());
        config.setAccessToken("abc");
        config.setEnvironmentName("integration-testing");
        return config;
    }

    @Produces
    public StuckThreadMonitorConfig buildStuckThreadMonitorConfig()
    {
        StuckThreadMonitorConfig config = new StuckThreadMonitorConfig();
        config.setThreshold(Duration.ofSeconds(1));
        config.setPeriod(100L);
        config.setPeriodTimeUnit(TimeUnit.MILLISECONDS);

        return config;
    }

}
