package org.cru.redegg.it;

import org.cru.redegg.recording.api.RedEgg;
import org.cru.redegg.test.DefaultDeployment;
import org.cru.redegg.test.TestApplication;
import org.cru.redegg.test.WebTargetBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * @author Matt Drees
 */

@RunWith(Arquillian.class)
public class EndToEndWithoutCdiIT extends AbstractEndToEndIT
{

    @Deployment
    public static WebArchive deployment()  {

        return DefaultDeployment.withoutCdi("end-to-end-test.war")
            .addAllRuntimeDependenciesExceptLog4j2()
            .getArchive()
            .addAsLibraries(RedEggDistribution.getJarFile())

            .addClass(TestApplication.class)
            .addClass(WebTargetBuilder.class)
            .addClass(AbstractApiThatErrors.class)
            .addClass(ApiWithoutCdiThatErrors.class)
            .addClass(DummyRollbarApi.class)
            .addClass(TestParameterSanitizer.class)
            .addClass(TestEntitySanitizer.class)
            .addClass(ConfigProducer.class)
            .addClass(Initializer.class)
            .addClass(AbstractEndToEndIT.class)
            ;
    }

    @WebListener
    public static class Initializer implements ServletContextListener
    {

        @Override
        public void contextInitialized(ServletContextEvent sce)
        {
            RedEgg.configure()
                .setParameterSanitizer(new TestParameterSanitizer())
                .setEntitySanitizer(new TestEntitySanitizer())
                .setRollbarConfig(new ConfigProducer().buildConfig());
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce)
        {
        }
    }

}
