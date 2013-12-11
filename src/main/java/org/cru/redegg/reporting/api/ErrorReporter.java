package org.cru.redegg.reporting.api;

import org.cru.redegg.reporting.ErrorReport;

/**
 * @author Matt Drees
 */
public interface ErrorReporter
{
    public void send(ErrorReport report);
}
