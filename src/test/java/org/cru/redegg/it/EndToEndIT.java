package org.cru.redegg.it;

import org.cru.redegg.reporting.errbit.ErrbitConfig;
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

import javax.enterprise.inject.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
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
public class EndToEndIT
{

    @Deployment
    public static WebArchive deployment()  {

        return new DefaultDeployment("end-to-end-test.war")
            .addAllRuntimeDependencies()
            .getArchive()
            //TODO: figure out how to not hard code the version here
            .addAsLibraries(new File("target/red-egg-1-SNAPSHOT.jar"))

            .addClass(TestApplication.class)
            .addClass(WebTargetBuilder.class)
            .addClass(ApiThatErrors.class)
            .addClass(DummyErrbitApi.class)
            .addClass(TestSanitizer.class)
            .addClass(ConfigProducer.class)
            ;
    }

    @ArquillianResource
    URL deploymentURL;

    @Test
    @RunAsClient
    public void testThrown() throws InterruptedException
    {
        WebTarget target = getWebTarget().path("explosions/throw");
        Form form = new Form()
            .param("secret", "letmein")
            .param("well-known-fact", "matt's a swell guy");
        Response appResponse = target
            .request()
            .header("secret", "ssshh")
            .header("answer-to-life-universe-and-everything", "42")
            .post(form(form));
        assertThat(appResponse.getStatus(), equalTo(500));

        waitABit();
        String report = getReport();

        assertThat(report, containsString("<api-key>abc</api-key>"));
        assertThat(report, containsString("kablooie!"));
        assertThat(report, containsString("matt's a swell guy"));
        assertThat(report, not(containsString("letmein")));
        assertThat(report, containsString("42"));
        assertThat(report, not(containsString("ssshh")));
    }

    @Test
    @RunAsClient
    public void testLogged() throws InterruptedException
    {
        WebTarget target = getWebTarget().path("explosions/log");
        Form form = new Form()
            .param("secret", "letmein")
            .param("better-known-fact", "matt's got a swell wife");
        Response appResponse = target
            .request()
            .post(form(form));
        assertThat(appResponse.getStatus(), equalTo(204));

        waitABit();
        String report = getReport();

        assertThat(report, containsString("<api-key>abc</api-key>"));
        assertThat(report, containsString("kablooie!"));
        assertThat(report, containsString("matt's got a swell wife"));
        assertThat(report, containsString("204"));
        assertThat(report, not(containsString("letmein")));
    }

    /**
     * On wildfly, this wait is necessary.
     * Otherwise the error queueing & reporting takes a hair longer than the tests' followup getReport() request,
     * and the dummy api won't have received the report yet.
     */
    private void waitABit() throws InterruptedException
    {
        TimeUnit.MILLISECONDS.sleep(500);
    }

    private String getReport()
    {
        Response reportResponse = getWebTarget().path("dummyapi/notices")
            .request()
            .get();
        String report = reportResponse.readEntity(String.class);

        assertThat(reportResponse.getStatus(), equalTo(200));

        System.out.println(report.replace("><", ">\n<"));
        return report;
    }

    private WebTarget getWebTarget()
    {
        return new WebTargetBuilder().getWebTarget(deploymentURL);
    }


    public static class ConfigProducer
    {
        @Produces
        public ErrbitConfig buildConfig() throws URISyntaxException
        {
            ErrbitConfig config = new ErrbitConfig();
            config.setEndpoint(new WebTargetBuilder().getWebTarget("end-to-end-test").path("/dummyapi/notices").getUri());
            config.setKey("abc");
            config.setEnvironmentName("integration-testing");
            config.getApplicationBasePackages().add("org.cru.redegg");
            config.setSourcePrefix("src/test/java");
            return config;
        }
    }

    @Path("/dummyapi/notices")
    public static class DummyErrbitApi
    {
        volatile static String report;

        @POST
        public void postNotice(String xmlPayload)
        {
            report = xmlPayload;
        }

        @GET
        public String getMostRecentReport()
        {
            return report;
        }

    }


}
