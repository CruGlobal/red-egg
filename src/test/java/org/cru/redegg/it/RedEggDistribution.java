package org.cru.redegg.it;

import org.cru.redegg.util.RedEggVersion;

import java.io.File;

/**
 * @author Matt Drees
 */
class RedEggDistribution
{

    public static File getJarFile()
    {
        return new File("target/red-egg-" + RedEggVersion.get() + ".jar");
    }

}
