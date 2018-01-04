package org.cru.redegg.recording.cdi;

import org.cru.redegg.qualifier.Fallback;
import org.cru.redegg.qualifier.Selected;
import org.cru.redegg.recording.StuckThreadMonitorConfig;
import org.cru.redegg.recording.api.EntitySanitizer;
import org.cru.redegg.recording.api.ParameterSanitizer;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

public class StuckThreadMonitorConfigProducer
{

    @Produces
    @Selected
    public StuckThreadMonitorConfig selectStuckThreadMonitorConfig(
        @Default Instance<StuckThreadMonitorConfig> defaultConfig,
        @Fallback StuckThreadMonitorConfig fallbackConfig)
    {
        if (!defaultConfig.isUnsatisfied())
            return defaultConfig.get();
        else
            return fallbackConfig;
    }

}
