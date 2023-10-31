package org.cru.redegg.recording.cdi;

import java.time.Clock;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import org.cru.redegg.qualifier.Fallback;
import org.cru.redegg.qualifier.Selected;
import org.cru.redegg.recording.StuckThreadMonitorConfig;

public class ClockProducer
{

    @Produces
    @Selected
    public Clock selectClock(
        @Default Instance<Clock> defaultClock)
    {
        if (!defaultClock.isUnsatisfied())
            return defaultClock.get();
        else
            return Clock.systemDefaultZone();
    }

}
