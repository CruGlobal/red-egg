package org.cru.redegg.it;

import com.google.common.collect.ImmutableList;
import org.cru.redegg.jaxrs.RecordingReaderInterceptor;
import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.ParameterSanitizer;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.recording.jul.RedEggHandler;
import org.cru.redegg.recording.log4j.RedEggAppender;
import org.cru.redegg.reporting.errbit.ErrbitConfig;
import org.cru.redegg.servlet.RedEggServletListener;
import org.cru.redegg.test.DefaultDeployment;
import org.cru.redegg.test.TestApplication;
import org.cru.redegg.util.Clock;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

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
            .addClass(ApiThatErrors.class)
            .addClass(DummyErrbitApi.class)
            .addClass(TestSanitizer.class)
            .addClass(ConfigProducer.class)
            ;
    }

    @Test
    @RunAsClient
    public void test()
    {
        WebTarget target = getWebTarget().path("explosions");
        Form form = new Form()
            .param("secret", "letmein")
            .param("well-known-fact", "matt's a swell guy");
        Response appResponse = target
            .request()
            .post(form(form));
        assertThat(appResponse.getStatus(), equalTo(500));

        Response reportResponse = getWebTarget().path("dummyapi/notices")
            .request()
            .get();
        assertThat(reportResponse.getStatus(), equalTo(200));

        String report = reportResponse.readEntity(String.class);

        System.out.println(report.replace("><", ">\n<"));

        assertThat(report, containsString("<api-key>abc</api-key>"));
        assertThat(report, containsString("kablooie!"));
        assertThat(report, containsString("matt's a swell guy"));
        assertThat(report, not(containsString("letmein")));

    }


    private WebTarget getWebTarget()
    {
        return ClientBuilder.newClient().target("http://localhost:8080/end-to-end-test/rest");
    }


    public static class ConfigProducer
    {
        @Produces
        public ErrbitConfig buildConfig() throws URISyntaxException
        {
            ErrbitConfig config = new ErrbitConfig();
            config.setEndpoint(new URI("http://localhost:8080/end-to-end-test/rest/dummyapi/notices"));
            config.setKey("abc");
            config.setEnvironmentName("integration-testing");
            return config;
        }
    }

    public static class TestSanitizer implements ParameterSanitizer
    {

        @Override
        public List<String> sanitizeQueryStringParameter(
            String parameterName, List<String> parameterValues)
        {
            return sanitize(parameterName, parameterValues);
        }

        @Override
        public List<String> sanitizePostBodyParameter(
            String parameterName, List<String> parameterValues)
        {
            return sanitize(parameterName, parameterValues);
        }

        private List<String> sanitize(String parameterName, List<String> parameterValues)
        {
            if (parameterName.equals("secret"))
                return ImmutableList.of("<redacted>");
            else
                return parameterValues;
        }
    }

    @Path("/explosions")
    public static class ApiThatErrors
    {

        @Inject
        //TODO: set up dependencies such that user can just inject ErrorRecorder
        WebErrorRecorder recorder;

        @POST
        public void boom()
        {
            recorder.recordContext("fun fact:", "I'm about to blow");
            throw new IllegalStateException("kablooie!");
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
