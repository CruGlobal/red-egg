package org.cru.redegg.reporting.rollbar;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.json.JsonSerializerImpl;
import org.cru.redegg.reporting.DummyReportBuilder;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.TestExceptions;
import org.cru.redegg.util.ErrorLog;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Matt Drees
 */
public class RollbarPayloadBuilderTest
{

    RollbarConfig config;
    ServletContext context;
    Payload payload;
    private ErrorReport report;
    private FileNameResolver resolver;


    @Before
    public void setup() throws Exception
    {
        config = new RollbarConfig();
        config.setEnvironmentName("unittest");
        config.setAccessToken("secrets");
        config.setBranch("master");
        config.setCodeVersion("11");
        config.setIdentifyingUserProperty("guid");

        context = Mockito.mock(ServletContext.class);
        resolver = new ServletContextFileNameResolver(context, new ErrorLog());

    }

    @Test
    public void testWriteJsonTo() throws IOException
    {
        buildSampleReport();
        payload = new RollbarPayloadBuilder(config, report).build();

        when(context.getMajorVersion()).thenReturn(3);
        when(context.getClassLoader()).thenReturn(getClass().getClassLoader());
        when(context.getResource("/WEB-INF/classes/org/cru/redegg/reporting/TestExceptions.class"))
            .thenReturn(new URL("file:dummy"));


//        Map<String, Object> src = payload.asJson();
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        gson.toJson(src, writer);
//

        String json = new JsonSerializerImpl(true).toJson(payload);

        checkJsonContent(json);
    }

    private void buildSampleReport()
    {
        report = new DummyReportBuilder().buildDummyReport();
    }

    private void checkJsonContent(String json)
    {
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("\"access_token\": \"secrets\""));
        assertThat(json, containsString("\"class\": \"java.lang.NullPointerException\""));
//        assertThat(json, containsString("\"message\": null"));
        assertThat(json, containsString("\"frames\": ["));
        assertThat(json, containsString("\"filename\": \"" + TestExceptions.simpleFilename() + "\""));
        assertThat(json, containsString("\"lineno\": 13"));
        assertThat(json, containsString("\"method\": \"boom\""));
        assertThat(json, containsString("java.lang.RuntimeException"));
        assertThat(json, containsString("\"context\": \"TestResource.doSomething(String)\""));

        // The rollbar UI renders query params ugly when sent as an array
        // (which rollbar-java does if there are more than 2),
        // but at least the common case is ok.
        assertThat(json, containsString(
            "\"lang\": [\n" +
            "          \"en_au\",\n" +
            "          \"en\"]"));
        assertThat(json, containsString("\"conferenceId\": \"10912\""));

        // custom server attribute
        assertThat(json, containsString("\"JAVA_OPTS\": \"-Xmx 512m\""));
        assertThat(json, containsString("\"X-Proxy\": \"proxy.somewhere.org,proxy.somewhereelse.org\""));
        assertThat(json, containsString("\"host\": \"testserver.cru.org\""));

        //custom person attribute
        assertThat(json, containsString("\"guid\": \"ABE4234-FASFA\""));
        assertThat(json, containsString("\"id\": \"ABE4234-FASFA\""));
        assertThat(json, containsString("\"framework\": \"[resteasy-jaxrs-3.0.5.Final, weld-core-1.1.16]\""));
        assertThat(json, endsWith("}"));
    }
}
