package org.cru.redegg.recording.jul;

import org.cru.redegg.recording.api.RecorderFactory;

import java.util.Set;

/**
 * @author Matt Drees
 */
public class JulRecorder
{
    private java.util.logging.Logger root;
    private RedEggHandler julHandler;

    public static JulRecorder add(RecorderFactory recorderFactory, Set<String> ignoredLoggers)
    {
        return new JulRecorder(recorderFactory, ignoredLoggers);
    }

    private JulRecorder(
        RecorderFactory recorderFactory,
        Set<String> ignoredLoggers)
    {
        julHandler = new RedEggHandler(recorderFactory, ignoredLoggers);
        root = java.util.logging.Logger.getLogger("");
        root.info("adding j.u.l. handler");
        root.addHandler(julHandler);

        if (root.getHandlers().length == 0)
            root.info("j.u.l. handlers appear to be disabled");

    }

    public void remove()
    {
        root.info("removing j.u.l. handler");
        root.removeHandler(julHandler);
    }
}
