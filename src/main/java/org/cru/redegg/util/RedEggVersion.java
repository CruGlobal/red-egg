package org.cru.redegg.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Matt Drees
 */
public class RedEggVersion
{
    private static final String VERSION;

    private static final Logger log = LoggerFactory.getLogger(RedEggVersion.class);

    static {
        String version;
        try
        {
            version = lookupVersionFromPom();
        }
        catch (Exception e)
        {
            version = "<unavailable>";
            log.error("can't lookup version from pom; using placeholder {} instead", version, e);
        }
        VERSION = version;
        log.info("Red Egg Version " + VERSION);
    }

    private static String lookupVersionFromPom()
    {
        Class<RedEggVersion> redEggClass = RedEggVersion.class;
        URL mavenPomLocation = redEggClass.getResource("/META-INF/maven/org.ccci/red-egg/pom.xml");
        if (mavenPomLocation == null)
        {
            // red egg is probably being run from the maven project layout
            URL classesLocation = redEggClass.getProtectionDomain().getCodeSource().getLocation();
            try
            {
                mavenPomLocation = new URL(classesLocation, "../../pom.xml");
            }
            catch (MalformedURLException e)
            {
                throw new RuntimeException(e);
            }

        }

        return new SimplePomParser(mavenPomLocation).getVersion();
    }

    public static String get()
    {
        return VERSION;
    }
}
