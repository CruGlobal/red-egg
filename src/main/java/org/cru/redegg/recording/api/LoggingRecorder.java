package org.cru.redegg.recording.api;

/**
 * @author Matt Drees
 */
public interface LoggingRecorder
{
    /**
     * Removes the recording logging appender/handler from the logging hierarchy.
     */
    void remove();
}
