package org.cru.redegg.it;

import org.cru.redegg.test.WebTargetBuilder;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.client.Entity.form;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Matt Drees
 */
public abstract class AbstractEndToEndIT
{

    @ArquillianResource
    URL deploymentURL;

    @Test
    @RunAsClient
    public void testFormThatThrowsException() throws InterruptedException
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

        String report = getReport();

        assertThat(report, containsString("\"access_token\":\"abc\""));
        assertThat(report, containsString("kablooie!"));
        assertThat(report, containsString("matt's a swell guy"));
        assertThat(report, not(containsString("letmein")));
        assertThat(report, containsString("42"));
        assertThat(report, not(containsString("ssshh")));
    }


    @Test
    @RunAsClient
    public void testJsonThatThrowsException() throws InterruptedException
    {
        WebTarget target = getWebTarget().path("explosions/throw");
        TestPayload payload = new TestPayload("letmein", "matt's a swell guy");
        Response appResponse = target
            .request()
            .header("secret", "ssshh")
            .header("answer-to-life-universe-and-everything", "42")
            .post(Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE));

        assertThat(appResponse.getStatus(), equalTo(500));

        String report = getReport();

        assertThat(report, containsString("\"access_token\":\"abc\""));
        assertThat(report, containsString("kablooie!"));
        assertThat(report, containsString("matt's a swell guy"));
        assertThat(report, not(containsString("letmein")));
        assertThat(report, containsString("42"));
        assertThat(report, not(containsString("ssshh")));
    }

    @XmlRootElement
    public static class TestPayload
    {
        public TestPayload(String secret, String wellKnownFact)
        {
            this.secret = secret;
            this.wellKnownFact = wellKnownFact;
        }

        public String secret;
        public String wellKnownFact;
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
        final Link link = appResponse.getLink("org.cru.links:error-details");
        assertThat(link, notNullValue());
        assertThat(link.getUri().toString(), startsWith("https://rollbar.com/occurrence/uuid/?uuid="));

        String report = getReport();

        assertThat(report, containsString("\"access_token\":\"abc\""));
        assertThat(report, containsString("kablooie!"));
        assertThat(report, containsString("matt's got a swell wife"));
        assertThat(report, containsString("204"));
        assertThat(report, not(containsString("letmein")));
    }

    String getReport() throws InterruptedException
    {
        WebTarget path = getWebTarget().path("dummyapi/notices");

        long currentTime = System.currentTimeMillis();
        Response reportResponse;
        do
        {
            waitABit();
            reportResponse = path.request().get();
            if (System.currentTimeMillis() > currentTime + 30 * 1000)
            {
                throw new AssertionError("A report wasn't sent within the required timeframe");
            }

        } while (reportResponse.getStatus() == 204);

        assertThat(reportResponse.getStatus(), equalTo(200));

        String report = reportResponse.readEntity(String.class);
        System.out.println(report.replace("><", ">\n<"));
        return report;
    }

    /**
     * Sometimes the error queueing & reporting takes a little longer than the tests' followup getReport() request,
     * and the dummy api won't have received the report yet.
     * So we need to wait a little.
     */
    private void waitABit() throws InterruptedException
    {
        TimeUnit.MILLISECONDS.sleep(500);
    }

    WebTarget getWebTarget()
    {
        return new WebTargetBuilder().getWebTarget(deploymentURL);
    }

}
