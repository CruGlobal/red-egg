package org.cru.redegg.it;

import org.cru.redegg.manual.Builder;
import org.cru.redegg.recording.api.RedEgg;
import org.cru.redegg.test.DefaultDeployment;
import org.cru.redegg.test.TestApplication;
import org.cru.redegg.test.WebTargetBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.client.Entity.form;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Matt Drees
 */

@RunWith(Arquillian.class)
public class EndToEndWithoutCdiIT extends AbstractEndToEndIT
{

    @Deployment
    public static WebArchive deployment()  {

        return DefaultDeployment.withoutCdi("end-to-end-test.war")
            .addAllRuntimeDependencies()
            .getArchive()
            //TODO: figure out how to not hard code the version here
            .addAsLibraries(new File("target/red-egg-1-SNAPSHOT.jar"))

            .addClass(TestApplication.class)
            .addClass(WebTargetBuilder.class)
            .addClass(AbstractApiThatErrors.class)
            .addClass(ApiWithoutCdiThatErrors.class)
            .addClass(DummyErrbitApi.class)
            .addClass(TestSanitizer.class)
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
            try
            {
                RedEgg.configure()
                    .setParameterSanitizer(new TestSanitizer())
                    .setErrbitConfig(new ConfigProducer().buildConfig());
            }
            catch (URISyntaxException e)
            {
                throw new AssertionError(e);
            }
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce)
        {
        }
    }

}
