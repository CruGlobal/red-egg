package org.cru.redegg.reporting.api;

import org.cru.redegg.reporting.ErrorReport;

import java.util.Optional;

/**
 * @author Matt Drees
 */
public interface ErrorQueue {

    /**
     * Puts an error report in the queue to be reported asynchronously.
     *
     * This doesn't throw an exception if the queue is full,
     * or if some other problem (a number-of-threads OME, for example)
     * prevents the queue from taking an additional element.
     */
    void enqueue(ErrorReport report);

    /**
     * Builds a link that can be attached to a ErrorReport,
     * if the underlying reporter supports such links.
     *
     * The link can be used in logs or http responses.
     */
    Optional<ErrorLink> buildLink();
}
