package org.cru.redegg.recording;

import org.cru.redegg.qualifier.Fallback;
import org.joda.time.Minutes;
import org.joda.time.ReadablePeriod;

import java.util.concurrent.TimeUnit;

@Fallback
public class StuckThreadMonitorConfig
{

    private static final ReadablePeriod DEFAULT_THRESHOLD = Minutes.minutes(30);
    private static final long DEFAULT_PERIOD_MINUTES = 5;

    private ReadablePeriod threshold = DEFAULT_THRESHOLD;

    private Long period = DEFAULT_PERIOD_MINUTES;

    private TimeUnit periodTimeUnit = TimeUnit.MINUTES;

    public ReadablePeriod getThreshold()
    {
        return threshold;
    }

    public void setThreshold(ReadablePeriod threshold)
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
