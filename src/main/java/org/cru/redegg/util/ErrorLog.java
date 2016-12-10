package org.cru.redegg.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Matt Drees
 */
public class ErrorLog
{

    private static final String NAME = "org.cru.redegg.internal.errors";
    public static String name()
    {
        return NAME;
    }

    final Logger log;

    public ErrorLog() {
        this.log = LoggerFactory.getLogger(NAME);
    }

    /**
     * will not trigger an exception, and will log the info if possible.
     */
    public void error(String s, Throwable t) {
        try
        {
            log.error(s,t);
        }
        catch (Throwable ignored)
        {
            //not sure what to do here.  Likely things are messed up enough that there's nothing reasonable to do.
        }
    }

    public void warn(String message)
    {
        log.warn(message);
    }
}
