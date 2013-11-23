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

    public void error(String s, Throwable t) {
        throw new UnsupportedOperationException();
    }
}
