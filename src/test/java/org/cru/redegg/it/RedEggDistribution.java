package org.cru.redegg.it;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * @author Matt Drees
 */
class RedEggDistribution
{

    public static File getJarFile()
    {
        File pomFile = new File("pom.xml");

        DefaultModelReader reader = new DefaultModelReader();
        Model model;
        try
        {
            model = reader.read(pomFile, Collections.<String, Object>emptyMap());
        }
        catch (IOException e)
        {
            throw new RuntimeException("unable to read pom.xml", e);
        }
        String version = model.getVersion();

        return new File("target/red-egg-" + version + ".jar");
    }

}
