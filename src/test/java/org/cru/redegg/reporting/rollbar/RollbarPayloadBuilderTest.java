package org.cru.redegg.reporting.rollbar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.rollbar.payload.Payload;
import com.rollbar.utilities.RollbarSerializer;
import org.cru.redegg.reporting.DummyReportBuilder;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.TestExceptions;
import org.cru.redegg.util.ErrorLog;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;

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

    StringWriter writer;
    RollbarConfig config;
    ServletContext context;
    Payload payload;
    private ErrorReport report;
    private FileNameResolver resolver;


    @Before
    public void setup() throws Exception
    {
        writer = new StringWriter();
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

        writer.write(new RollbarSerializer(true).serialize(payload));

        checkJsonContent();
    }

    private void buildSampleReport()
    {
        report = new DummyReportBuilder().buildDummyReport();
    }

    private void checkJsonContent()
    {
        String json = writer.toString();

        assertThat(json, startsWith("{"));
        assertThat(json, containsString("\"access_token\": \"secrets\""));
        assertThat(json, containsString("\"class\": \"java.lang.NullPointerException\""));
        assertThat(json, containsString("\"message\": null"));
        assertThat(json, containsString("\"frames\": ["));
        assertThat(json, containsString("\"filename\": \"" + TestExceptions.simpleFilename() + "\""));
        assertThat(json, containsString("\"lineno\": 13"));
        assertThat(json, containsString("\"method\": \"boom\""));
        assertThat(json, containsString("java.lang.RuntimeException"));
        assertThat(json, containsString("\"context\": \"TestResource.doSomething(String)\""));
        assertThat(json, containsString("\"lang\": \"en_au,en\""));
        assertThat(json, containsString("\"conferenceId\": \"10912\""));
        assertThat(json, containsString("\"JAVA_OPTS\": \"-Xmx 512m\""));
        assertThat(json, containsString("\"X-Proxy\": \"proxy.somewhere.org,proxy.somewhereelse.org\""));
        assertThat(json, containsString("\"host\": \"testserver.cru.org\""));
        assertThat(json, containsString("\"guid\": \"ABE4234-FASFA\""));
        assertThat(json, containsString("\"id\": \"ABE4234-FASFA\""));
        assertThat(json, containsString("\"framework\": \"[resteasy-jaxrs-3.0.5.Final, weld-core-1.1.16]\""));
        assertThat(json, endsWith("}"));
    }
}
