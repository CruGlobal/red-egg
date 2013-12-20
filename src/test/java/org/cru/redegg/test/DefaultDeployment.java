package org.cru.redegg.test;

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
        this(null);
    }

    public DefaultDeployment(String archiveName) {
        resolver = Maven.resolver().offline().loadPomFromFile("pom.xml");

        webArchive = getWebArchive(archiveName)
           .addAsWebInfResource("beans.xml");

        addLibraries(
           "org.mockito:mockito-core",
           "uk.co.datumedge:hamcrest-json",
           "com.google.guava:guava",
           "joda-time:joda-time"
        );

    }

    private WebArchive getWebArchive(String archiveName)
    {
        return archiveName == null ?
            ShrinkWrap.create(WebArchive.class) :
            ShrinkWrap.create(WebArchive.class, archiveName);
    }

    private void addLibraries(String... libraryCoordinates)
    {
        webArchive.addAsLibraries(resolver
            .resolve(libraryCoordinates)
            .withTransitivity()
            .asFile());
    }

    public DefaultDeployment addAllRuntimeDependencies()
    {
        webArchive.addAsLibraries(
            resolver.importRuntimeDependencies()
                .resolve()
                .withTransitivity()
                .asFile());
        return this;
    }


    public WebArchive getArchive() {
        return webArchive;
    }

}
