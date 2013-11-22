package org.cru.redegg.util;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

/**
 * Acts a little like JSR 310 Clock: http://threeten.sourceforge.net/apidocs/javax/time/calendar/Clock.html
 * (except simpler)
 *
 * May be subclassed for tests which want to manipulate time. see http://tech.puredanger.com/2008/09/24/controlling-time/
 *
 * Should be replaced by JSR 310's Clock whenever JSR 310 is released
 *
 * @author Matt Drees
 *
 */
public abstract class Clock
{

    public abstract DateTime dateTime();

    public LocalDate today()
    {
        return new LocalDate(dateTime());
    }

    public static Clock system()
    {
        return new SystemClock();
    }

    static class SystemClock extends Clock
    {

        @Override
        public DateTime dateTime() {
            return new DateTime();
        }

    }

}