package org.cru.redegg.recording;

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import org.cru.redegg.qualifier.Fallback;

import java.util.concurrent.TimeUnit;

@Fallback
public class StuckThreadMonitorConfig
{

    private static final Duration DEFAULT_THRESHOLD = Duration.ofMinutes(30);
    private static final long DEFAULT_PERIOD_MINUTES = 5;

    private Duration threshold = DEFAULT_THRESHOLD;

    private Long period = DEFAULT_PERIOD_MINUTES;

    private TimeUnit periodTimeUnit = TimeUnit.MINUTES;

    public TemporalAmount getThreshold()
    {
        return threshold;
    }

    public void setThreshold(Duration threshold)
    {
        this.threshold = threshold;
    }

    public Long getPeriod()
    {
        return period;
    }

    public void setPeriod(Long period)
    {
        this.period = period;
    }

    public TimeUnit getPeriodTimeUnit()
    {
        return periodTimeUnit;
    }

    public void setPeriodTimeUnit(TimeUnit periodTimeUnit)
    {
        this.periodTimeUnit = periodTimeUnit;
    }
}
