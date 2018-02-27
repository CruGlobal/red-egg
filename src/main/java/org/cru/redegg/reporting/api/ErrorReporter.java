package org.cru.redegg.reporting.api;

import org.cru.redegg.reporting.ErrorReport;

import java.util.Optional;

/**
 * @author Matt Drees
 */
public interface ErrorReporter
{
    void send(ErrorReport report);

    Optional<ErrorLink> buildLink();
}
