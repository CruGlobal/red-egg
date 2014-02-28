package org.cru.redegg.boot;

import com.google.common.base.Objects;
import org.cru.redegg.manual.Builder;
import org.cru.redegg.servlet.RedEggFilter;
import org.cru.redegg.servlet.RedEggServletListener;

import javax.servlet.ServletContext;
import java.util.logging.Logger;

/**
 * @author Matt Drees
 */
public class Initializer
{

    private static Logger logger = Logger.getLogger(Initializer.class.getName());

    public static void initializeIfNecessary(RedEggFilter filter, ServletContext servletContext)
    {
        if (manualInitializationNeeded(servletContext))
        {
            Builder.getInstance().init(filter);
        }
    }

    public static void initializeIfNecessary(RedEggServletListener listener, ServletContext servletContext)
    {
        if (manualInitializationNeeded(servletContext))
        {
            logger.info("using manual injection mode");
            Builder.getInstance().init(listener);
        }
    }

    private static boolean manualInitializationNeeded(ServletContext servletContext)
    {
        return Objects.equal(servletContext.getInitParameter("org.cru.redegg.no-cdi"), "true");
    }
}
