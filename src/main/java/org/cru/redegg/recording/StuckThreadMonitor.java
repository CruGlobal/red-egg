package org.cru.redegg.recording;

import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.cru.redegg.reporting.api.ErrorQueue;

/**
 * A tool for monitoring active requests, and notifying if any request seems 'stuck'
 * (i.e. it takes too long to complete).
 *
 * @author Matt Drees
 */
public interface StuckThreadMonitor
{
    /**
     * Begin monitoring this request (represented by a WebContext instance).
     * If enough time passes between the request start time and the current clock time,
     * and if {@link #finishMonitoringRequest(WebContext)} has not been called,
     * then an {@link ErrorReport} is created with a stack trace of the request's thread
     * and is enqueued in the {@link ErrorQueue}.
     *
     * The error report won't necessarily be as rich in information
     * as what would be created by a typical in-request exception.
     * For simplicity and threadsafety,
     * only the web context info available at the start of the request is recorded.
     * User info and app context info,
     * which is typically recorded during the 'application' portion of the request,
     * is not available to the stuck thread monitor.
     *
     * @param webContext request info that will be included in the generated ErrorReport.
     */
    void startMonitoringRequest(WebContext webContext);

    /**
     * Finish monitoring this request.
     *
     * {@link #startMonitoringRequest(WebContext)} must have been called beforehand,
     * with the exact same argument.
     *
     * @param webContext
     */
    void finishMonitoringRequest(WebContext webContext);
}
