package org.cru.redegg;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;

import java.io.File;

/**
 * @author Matt Drees
 */
public class DefaultDeployment {

    private PomEquippedResolveStage resolver;
    private WebArchive webArchive;

    public DefaultDeployment() {

        try
        {
            resolver = Maven.resolver().offline().loadPomFromFile("pom.xml");
        }
        catch (Throwable e)
        // arquillian was not displaying the stacktrace from an exception thrown from above, so we
        // display stacktraces manually
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        webArchive = ShrinkWrap.create(WebArchive.class)
            .addAsWebInfResource("beans.xml");

        addLibraries(
            "org.mockito:mockito-core",
            "com.google.guava:guava",
            "joda-time:joda-time"
//            "org.ccci:atlassian-hamcrest",
//            "org.hamcrest:hamcrest-library"
        );
    }


    private void addLibraries(String... libraryCoordinates)
    {
        webArchive.addAsLibraries(resolver
            .resolve(libraryCoordinates)
            .withTransitivity()
            .asFile());
    }


    public WebArchive getArchive() {
        return webArchive;
    }

}
