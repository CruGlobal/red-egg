package org.cru.redegg.recording.api;

/**
 * @author Matt Drees
 */
public interface RecorderFactory {

    public ErrorRecorder getRecorder() ;
    public WebErrorRecorder getWebRecorder() ;
}
