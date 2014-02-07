package org.cru.redegg;


import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.cru.redegg.boot.Lifecycle;
import org.cru.redegg.jaxrs.RecordingReaderInterceptor;
import org.cru.redegg.recording.api.NoOpParameterSanitizer;
import org.cru.redegg.recording.api.ParameterSanitizer;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.recording.jul.RedEggHandler;
import org.cru.redegg.recording.log4j.RedEggAppender;
import org.cru.redegg.servlet.RedEggServletListener;
import org.cru.redegg.test.AnswerWithSelf;
import org.cru.redegg.test.DefaultDeployment;
import org.cru.redegg.test.TestApplication;
import org.cru.redegg.test.WebTargetBuilder;
import org.cru.redegg.util.Clock;
import org.hamcrest.Matcher;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.ClientBuilder;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

@RunWith(Arquillian.class)
public class JaxrsRecordingIntegrationTest
{
    @Deployment
    public static WebArchive deployment()  {

        return new DefaultDeployment("jaxrs-test.war")
            .getArchive()
            .addPackage(RedEggServletListener.class.getPackage())
            .addPackage(Lifecycle.class.getPackage())
            .addPackage(Clock.class.getPackage())
            .addPackage(RecorderFactory.class.getPackage())
            .addPackage(RedEggHandler.class.getPackage())
            .addPackage(RedEggAppender.class.getPackage())

            .addPackage(RecordingReaderInterceptor.class.getPackage())

            .addClass(TestApplication.class)
            .addClass(WebTargetBuilder.class)
            .addClass(Fruit.class)
            .addClass(FruitResource.class)
            .addClass(AnswerWithSelf.class)
           ;
    }

    @Inject
    WebErrorRecorder recorder;

    @Inject Mocks mocks;

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
        verify(recorder).recordRequestPostParameters(ImmutableMultimap.<String, String>of());
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

    private WebTarget getWebTarget()
    {
        return new WebTargetBuilder()
               .getWebTarget("jaxrs-test")
               .path("fruits");
    }



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

        @PUT
        @javax.ws.rs.Produces({"application/json", "application/xml"})
        @Consumes({"application/json", "application/xml"})
        public Fruit putFruit(Fruit fruit)
        {
            return fruit;
        }

    }

    @ApplicationScoped
    public static class Mocks
    {

        @Produces
        @Mock
        RecorderFactory factory;

        @Produces
        WebErrorRecorder recorder;

        @Produces
        ParameterSanitizer sanitizer = new NoOpParameterSanitizer();

        @PostConstruct
        public void init()
        {
            MockitoAnnotations.initMocks(this);
            recorder = mock(WebErrorRecorder.class, new AnswerWithSelf(WebErrorRecorder.class));
            reset();
        }

        public void reset()
        {
            // we use Mockito.reset() instead of building new mocks, because the servlet listener is only initialized
            // once for this test class, and there is no easy way to modify its reference to a new mock
            Mockito.reset(recorder, factory);
            when(factory.getRecorder()).thenReturn(recorder);
            when(factory.getWebRecorder()).thenReturn(recorder);
        }

    }


}
