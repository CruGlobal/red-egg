package org.cru.redegg.util;

import org.apache.log4j.Logger;

/**
 * @author Matt Drees
 */
public class Log {

    final Logger log;

    public Log(Logger log) {
        this.log = log;
    }

    public Log() {
        //temporarily
        this.log = Logger.getLogger(getClass());
    }

    /**
     * will not trigger an exception, and will log the info if possible.
     */
    public void error(String s, Throwable t) {

    }
}
