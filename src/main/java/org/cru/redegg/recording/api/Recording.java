package org.cru.redegg.recording.api;

import org.cru.redegg.recording.api.ErrorRecorder;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Matt Drees
 */
public class Recording {

    public static ErrorRecorder getRecorder(HttpServletRequest request) {
        throw new UnsupportedOperationException();
    }
}
