package org.cru.redegg.it;

import org.cru.redegg.recording.impl.DefaultStuckThreadMonitor;
import org.cru.redegg.test.DefaultDeployment;
import org.cru.redegg.test.TestApplication;
import org.cru.redegg.test.WebTargetBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import static javax.ws.rs.client.Entity.form;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Matt Drees
 */

@RunWith(Arquillian.class)
@RunAsClient
public class EndToEndWithCdiIT extends AbstractEndToEndIT
{

    @Deployment
    public static WebArchive deployment()  {

        return DefaultDeployment.withCdi("end-to-end-test.war")
            .addAllRuntimeDependenciesExceptLog4j2()
            .getArchive()
            .addAsLibraries(RedEggDistribution.getJarFile())

            .addClass(TestApplication.class)
            .addClass(WebTargetBuilder.class)
            .addClass(AbstractApiThatErrors.class)
            .addClass(ApiWithCdiThatErrors.class)
            .addClass(DummyRollbarApi.class)
            .addClass(TestParameterSanitizer.class)
            .addClass(TestEntitySanitizer.class)
            .addClass(ConfigProducer.class)
            .addClass(AbstractEndToEndIT.class)
            ;
    }


    /* This test depends on CDI,
     * since non-cdi config is not implemented for the stuck thread monitor
     */
    @Test
    @RunAsClient
    public void testStuck() throws InterruptedException
    {
        WebTarget target = getWebTarget().path("explosions/hang");
        Form form = new Form()
            .param("secret", "letmein")
            .param("note", "this response should be too slow");
        Response appResponse = target
            .request()
            .post(form(form));
        assertThat(appResponse.getStatus(), equalTo(204));

        String report = getReport();

        assertThat(report, containsString("\"access_token\":\"abc\""));
        assertThat(report, containsString(DefaultStuckThreadMonitor.StuckThreadException.class.getName()));
        assertThat(report, containsString("this response should be too slow"));
        assertThat(report, not(containsString("204")));
        assertThat(report, not(containsString("letmein")));
    }

}
