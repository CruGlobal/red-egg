package org.cru.redegg.test;

import org.cru.redegg.boot.Lifecycle;
import org.cru.redegg.qualifier.Selected;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.cdi.SanitizerProducer;
import org.cru.redegg.recording.impl.HyperConservativeEntitySanitizer;
import org.cru.redegg.recording.impl.HyperConservativeParameterSanitizer;
import org.cru.redegg.recording.interceptor.ActionRecordingInterceptor;
import org.cru.redegg.recording.jul.RedEggHandler;
import org.cru.redegg.recording.log4j.RedEggAppender;
import org.cru.redegg.servlet.RedEggServletListener;
import org.cru.redegg.util.Clock;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;

/**
 * @author Matt Drees
 */
public class DefaultDeployment {

    private PomEquippedResolveStage resolver;
    private WebArchive webArchive;

    public static DefaultDeployment withCdi() {
        return withCdi(null);
    }

    public static DefaultDeployment withoutCdi(String archiveName)
    {
        DefaultDeployment deployment = new DefaultDeployment(archiveName);
        deployment.addNoCdiWebXml();

        /* This is a little odd for a 'without cdi' test, but we need it.
         * Without it, if the container we are testing against has CDI capabilities,
         * the red-egg jar will fail the deployment because of unsatisified dependencies.
         * I want the testsuite to be able to be fully run against one container,
         * and that means the container must have CDI.
         */
        deployment.addBeansXml();

        return deployment;
    }

    private void addNoCdiWebXml()
    {
        webArchive.addAsWebInfResource("no-cdi-web.xml", "web.xml");
    }


    public static DefaultDeployment withCdi(String archiveName)
    {
        DefaultDeployment deployment = new DefaultDeployment(archiveName);
        deployment.addBeansXml();
        deployment.getArchive()
            .addClass(ActionRecordingInterceptor.class)
            .addPackage(qualifier());
        return deployment;
    }

    private static Package qualifier()
    {
        return Selected.class.getPackage();
    }

    private void addBeansXml()
    {
        webArchive.addAsWebInfResource("beans.xml");
    }


    private DefaultDeployment(String archiveName) {
        resolver = Maven.resolver().offline().loadPomFromFile("pom.xml");

        webArchive = getWebArchive(archiveName);

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

    /**
     * Adds the boot, servlet, util, api, jul, and log4j packages
     */
    public DefaultDeployment addCorePackages()
    {
        getArchive()
            .addPackage(boot())
            .addPackage(servlet())
            .addPackage(util())
            .addPackage(recordingApi())
            .addPackage(recordingJul())
            .addPackage(recordingLog4j());
        return this;
    }

    private Package boot()
    {
        return Lifecycle.class.getPackage();
    }

    private Package servlet()
    {
        return RedEggServletListener.class.getPackage();
    }

    private Package util()
    {
        return Clock.class.getPackage();
    }

    private Package recordingApi()
    {
        return RecorderFactory.class.getPackage();
    }

    private Package recordingJul()
    {
        return RedEggHandler.class.getPackage();
    }

    private Package recordingLog4j()
    {
        return RedEggAppender.class.getPackage();
    }

    public DefaultDeployment addRecordingSanitizerClasses()
    {
        getArchive()
            .addClass(HyperConservativeEntitySanitizer.class)
            .addClass(HyperConservativeParameterSanitizer.class)
            .addClass(SanitizerProducer.class);
        return this;
    }
}
