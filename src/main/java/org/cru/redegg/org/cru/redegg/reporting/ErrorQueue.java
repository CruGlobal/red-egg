package org.cru.redegg.org.cru.redegg.reporting;

/**
 * @author Matt Drees
 */
public interface ErrorQueue {

    public void enqueue(ErrorReport report);

}
