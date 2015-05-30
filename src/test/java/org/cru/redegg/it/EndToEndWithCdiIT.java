package org.cru.redegg.it;

import org.cru.redegg.test.DefaultDeployment;
import org.cru.redegg.test.TestApplication;
import org.cru.redegg.test.WebTargetBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import java.io.File;

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
            .addAllRuntimeDependencies()
            .getArchive()
            //TODO: figure out how to not hard code the version here
            .addAsLibraries(new File("target/red-egg-1-SNAPSHOT.jar"))

            .addClass(TestApplication.class)
            .addClass(WebTargetBuilder.class)
            .addClass(AbstractApiThatErrors.class)
            .addClass(ApiWithCdiThatErrors.class)
            .addClass(DummyErrbitApi.class)
            .addClass(TestParameterSanitizer.class)
            .addClass(TestEntitySanitizer.class)
            .addClass(ConfigProducer.class)
            .addClass(AbstractEndToEndIT.class)
            ;
    }


}
