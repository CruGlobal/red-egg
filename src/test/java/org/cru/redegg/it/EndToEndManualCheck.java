package org.cru.redegg.it;

import com.google.gson.JsonObject;
import org.cru.redegg.reporting.rollbar.RollbarConfig;
import org.cru.redegg.test.DefaultDeployment;
import org.cru.redegg.test.TestApplication;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.inject.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.client.Entity.form;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This allows you to test against the rollbar api.
 * It is not run as part of the build.
 *
 * The appserver must be running with this system property defined:
 * * rollbar.accessToken
 *
 * @author Matt Drees
 */

@RunWith(Arquillian.class)
public class EndToEndManualCheck
{

    @Deployment
    public static WebArchive deployment()  {

        return DefaultDeployment.withCdi("end-to-end-test.war")
            .addAllRuntimeDependenciesExceptLog4j2()
            .getArchive()
            .addAsLibraries(RedEggDistribution.getJarFile())

            .addClass(TestApplication.class)
            .addClass(AbstractApiThatErrors.class)
            .addClass(ApiWithCdiThatErrors.class)
            .addClass(TestParameterSanitizer.class)
            .addClass(ConfigProducer.class)
            ;
    }

    @AfterClass
    public static void waitForAsyncQueueToSendToRollbar() throws InterruptedException
    {
        TimeUnit.SECONDS.sleep(2);
    }

    @Test
    @RunAsClient
    public void testThrownWithFormBody()
    {
        WebTarget target = getWebTarget().path("explosions/throw")
            .queryParam("explosion-size", "on-the-smaller-side");
        Form form = new Form()
            .param("secret", "letmein")
            .param("well-known-fact", "matt's a swell guy");
        Response appResponse = target
            .request()
            .post(form(form));
        assertThat(appResponse.getStatus(), equalTo(500));
    }

    @Test
    @RunAsClient
    public void testThrownWithJsonBody()
    {
        WebTarget target = getWebTarget().path("explosions/throw")
            .queryParam("explosion-size", "on-the-smaller-side");
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("secret", "letmein");
        jsonObject.addProperty("well-known-fact", "matt's a swell guy");
        Entity<String> entity =
            Entity.entity(jsonObject.toString(), MediaType.APPLICATION_JSON_TYPE);
        Response appResponse = target
            .request()
            .post(entity);
        assertThat(appResponse.getStatus(), equalTo(500));
    }

    @Test
    @RunAsClient
    public void testLogged()
    {
        WebTarget target = getWebTarget().path("explosions/log");
        Form form = new Form()
            .param("secret", "letmein")
            .param("better-known-fact", "matt's got a swell wife");
        Response appResponse = target
            .request()
            .post(form(form));
        assertThat(appResponse.getStatus(), equalTo(204));
    }


    private WebTarget getWebTarget()
    {
        return ClientBuilder.newClient().target("http://localhost:8080/end-to-end-test/rest");
    }


    public static class ConfigProducer
    {
        @Produces
        public RollbarConfig buildConfig() throws URISyntaxException
        {
            RollbarConfig config = new RollbarConfig();
            config.setAccessToken(System.getProperty("rollbar.accessToken"));
            config.setEnvironmentName("manual-testing");
            return config;
        }
    }

}
