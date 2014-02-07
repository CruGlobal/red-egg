package org.cru.redegg.boot;

import org.apache.log4j.Logger;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.jul.RedEggHandler;
import org.cru.redegg.recording.log4j.RedEggAppender;
import org.cru.redegg.util.ProxyConstructor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Enumeration;

/**
 * @author Matt Drees
 */
@ApplicationScoped
public class Lifecycle
{
    private final RecorderFactory recorderFactory;

    @Inject
    public Lifecycle(RecorderFactory recorderFactory)
    {
        this.recorderFactory = recorderFactory;
    }

    @ProxyConstructor
    @SuppressWarnings("UnusedDeclaration")
    Lifecycle()
    {
        recorderFactory = null;
    }

    private Logger log4jRoot;
    private java.util.logging.Logger julRoot;
    private RedEggAppender log4jAppender;
    private RedEggHandler julHandler;

    public void beginApplication()
    {
        addLog4jAppender();
        addJulHandler();
    }

    public void endApplication()
    {
        removeLog4jAppender();
        removeJulHandler();
    }

    private void addLog4jAppender() {
        log4jAppender = new RedEggAppender(recorderFactory);
        log4jRoot = Logger.getRootLogger();
        log4jRoot.info("adding log4j appender");
        log4jRoot.addAppender(log4jAppender);
        Enumeration allAppenders = log4jRoot.getAllAppenders();
        boolean none = !allAppenders.hasMoreElements();
        if (none)
            log4jRoot.info("log4j appenders appear to be disabled");
    }

    private void addJulHandler() {
        julHandler = new RedEggHandler(recorderFactory);
        julRoot = java.util.logging.Logger.getLogger("");
        julRoot.info("adding j.u.l. handler");
        julRoot.addHandler(julHandler);

        if (julRoot.getHandlers().length == 0)
            julRoot.info("j.u.l. handlers appear to be disabled");
    }

    private void removeJulHandler()
    {
        julRoot.info("removing j.u.l. handler");
        julRoot.removeHandler(julHandler);
    }

    private void removeLog4jAppender()
    {
        log4jRoot.info("removing log4j appender");
        log4jRoot.removeAppender(log4jAppender);
    }

    public void beginRequest()
    {
    }

    public void endRequest()
    {
    }

}
