package org.cru.redegg;


import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.cru.redegg.jaxrs.RecordingReaderInterceptor;
import org.cru.redegg.qualifier.EntityStreamPreservation;
import org.cru.redegg.recording.api.RequestMatcher;
import org.cru.redegg.recording.api.RequestMatchers;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.reporting.LoggingReporter;
import org.cru.redegg.reporting.api.ErrorReporter;
import org.cru.redegg.test.AnswerWithSelf;
import org.cru.redegg.test.DefaultDeployment;
import org.cru.redegg.test.TestApplication;
import org.cru.redegg.test.WebTargetBuilder;
import org.hamcrest.Matcher;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.client.Entity.form;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

@RunWith(Arquillian.class)
public class JaxrsRecordingIntegrationTest
{
    @Deployment
    public static WebArchive deployment()  {

        return DefaultDeployment.withCdi("jaxrs-test.war")
            .addCoreWildflyPackages()
            .addRecordingConfigurationClasses()
            .getArchive()

            .addPackage(RecordingReaderInterceptor.class.getPackage())

            .addClass(LoggingReporter.class)
            .addClass(ErrorReporter.class)

            .addClass(TestApplication.class)
            .addClass(WebTargetBuilder.class)
            .addClass(AnswerWithSelf.class)
            .addClass(RecordingMocks.class)
           ;
    }

    @Inject
    WebErrorRecorder recorder;

    @Inject RecordingMocks mocks;

    @Before
    public void setup()
    {
        mocks.reset();
    }

    @Test
    public void testSimpleJsonJaxrsGetRequest() throws Exception {
        WebTarget target = getWebTarget();
        Response response = target
            .queryParam("color", "red")
            .request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .get();
        assertThat(response.getStatus(), equalTo(200));

        response.readEntity(Fruit.class);


        verify(recorder, never()).recordEntityRepresentation(anyString());

        Multimap<String, String> expected = LinkedHashMultimap.create();
        expected.put("color", "red");
        verify(recorder).recordRequestQueryParameters(expected);
        verify(recorder).recordResponseStatus(200);
        verify(recorder).recordComponent(FruitResource.class.getMethod("getFruit", String.class));
    }


    @Test
    public void testSimpleJsonJaxrsDeleteRequest() throws Exception {
        WebTarget target = getWebTarget();
        Response response = target
            .queryParam("color", "red")
            .request()
            .delete();
        assertThat(response.getStatus(), equalTo(200));

        verify(recorder, never()).recordEntityRepresentation(anyString());

        Multimap<String, String> expected = LinkedHashMultimap.create();
        expected.put("color", "red");
        verify(recorder).recordRequestQueryParameters(expected);
        verify(recorder).recordComponent(FruitResource.class.getMethod("deleteFruit", String.class));
    }

    @Test
    public void testSimpleJsonJaxrsPutRequest() throws Exception {
        WebTarget target = getWebTarget();
        Fruit fruit = new Fruit("orange", 3.3);
        Response response = target
            .request()
            .put(entity(fruit, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), equalTo(200));

        @SuppressWarnings("unchecked")
        Matcher<String> matchesExpectedJson = (Matcher<String>) sameJSONAs(
            "{" +
            "\"color\": \"orange\"," +
            "\"weight\": 3.3" +
            "}");
        verify(recorder).recordEntityRepresentation(argThat(matchesExpectedJson));
    }

    @Test
    public void testSimpleJsonJaxrsPostJsonRequest() throws Exception {
        WebTarget target = getWebTarget();
        Fruit fruit = new Fruit("orange", 3.3);
        Response response = target
            .request()
            .post(entity(fruit, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), equalTo(200));

        verify(recorder).recordEntityRepresentation(anyString());

        ImmutableMultimap<String, String> expected = ImmutableMultimap.of();
        /* Note: it would be nice to verify this happen occurred exactly once,
         * but unfortunately arquillian occasionally makes its own POST requests
         * (with no post parameters) to the ArquillianServletRunner during test executions.
         * This causes the recorder to record more than one request,
         * which would fail that verification.
         * So, we use atLeastOnce() which is good enough.
         */
        verify(recorder, atLeastOnce()).recordRequestPostParameters(expected);
    }

    @Test
    public void testSimpleJsonJaxrsPostFormRequest() throws Exception {
        WebTarget target = getWebTarget();
        Form fruit = new Form()
            .param("color", "orange")
            .param("weight", "3.3");
        Response response = target
            .request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(form(fruit));
        assertThat(response.getStatus(), equalTo(200));

        response.readEntity(Fruit.class);

        verify(recorder, never()).recordEntityRepresentation(anyString());

        Multimap<String, String> expected = LinkedHashMultimap.create();
        expected.put("color", "orange");
        expected.put("weight", "3.3");
        verify(recorder).recordRequestPostParameters(expected);
    }

    @Test
    public void testSimpleJsonJaxrsPostFormRequestForRawProcessing() throws Exception {
        WebTarget target = getWebTarget().path("raw");

        Form fruit = new Form()
            .param("color", "orange")
            .param("weight", "3.3");
        Response response = target
            .request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(form(fruit));
        assertThat(response.getStatus(), equalTo(204));

        verify(recorder).recordEntityRepresentation(anyString());

        Multimap<String, String> expected = ImmutableMultimap.of();
        // See note in testSimpleJsonJaxrsPostJsonRequest()
        verify(recorder, atLeastOnce()).recordRequestPostParameters(expected);
    }

    private WebTarget getWebTarget()
    {
        return new WebTargetBuilder()
               .getWebTarget("jaxrs-test")
               .path("fruits");
    }



    @SuppressWarnings("UnusedDeclaration")
    @XmlRootElement
    public static class Fruit
    {
        private String color;
        private double weight;

        public Fruit() {}

        public Fruit(String color, double weight)
        {
            this.color = color;
            this.weight = weight;
        }

        public String getColor()
        {
            return color;
        }

        public void setColor(String color)
        {
            this.color = color;
        }

        public double getWeight()
        {
            return weight;
        }

        public void setWeight(double weight)
        {
            this.weight = weight;
        }
    }


    @Path("fruits")
    public static class FruitResource
    {
        @GET
        @javax.ws.rs.Produces({"application/json", "application/xml"})
        public Fruit getFruit(@QueryParam("color") String color)
        {
            return new Fruit(color, 2.3);
        }

        @DELETE
        @javax.ws.rs.Produces({"text/plain"})
        public String deleteFruit(@QueryParam("color") String color)
        {
            return "success";
        }

        @POST
        @javax.ws.rs.Produces({"application/json", "application/xml"})
        @Consumes({"application/json", "application/xml"})
        public Fruit postFruit(Fruit fruit)
        {
            return fruit;
        }

        @POST
        @javax.ws.rs.Produces({"application/json", "application/xml"})
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public Fruit postFruit(@FormParam("color") String color, @FormParam("weight") double weight)
        {
            return new Fruit(color, weight);
        }

        @Path("/raw")
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public void postFruitWithRawHandling(String rawContentAsString)
        {
            String expectedRawContent = "color=orange&weight=3.3";
            assertThat(rawContentAsString, equalTo(expectedRawContent));
        }

        @PUT
        @javax.ws.rs.Produces({"application/json", "application/xml"})
        @Consumes({"application/json", "application/xml"})
        public Fruit putFruit(Fruit fruit)
        {
            return fruit;
        }

    }

    @SuppressWarnings("UnusedDeclaration")
    public static class EntityPreservingRequestMatcherProducer
    {
        @javax.enterprise.inject.Produces
        @Default
        @EntityStreamPreservation
        public RequestMatcher matcher = RequestMatchers.matchingPaths("/.*/raw");
    }
}
