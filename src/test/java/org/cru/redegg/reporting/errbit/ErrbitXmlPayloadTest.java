package org.cru.redegg.reporting.errbit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Matt Drees
 */
public class ErrbitXmlPayloadTest
{

    StringWriter writer;
    ErrbitConfig config;
    private ErrorReport report;


    @Before
    public void setup()
    {
        writer = new StringWriter();
        config = new ErrbitConfig();
        config.setEnvironmentName("unittest");
        config.setKey("secrets");
        config.getApplicationBasePackages().add("org.cru.redegg");
        config.setSourcePrefix("src/test/java");
    }

    @Test
    public void testWriteXmlTo() throws Exception
    {
        buildSampleReport();
        ErrbitXmlPayload payload = new ErrbitXmlPayload(report, config);
        payload.writeXmlTo(writer);
        checkXmlContent();
    }

    private void buildSampleReport() throws Exception
    {
        report = new ErrorReport();
        report.setLocalHostName("testserver.cru.org");
        report.setLocalHostAddress("10.10.10.10");
        List<String> logRecords = ImmutableList.of(
            "03:41:42.004 - WARN - Things looking bad...",
            "03:41:42.292 - ERROR - Yep, definitely bad!"
        );
        report.setLogRecords(logRecords);
        report.setEnvironmentVariables(
            ImmutableMap.of(
                "JAVA_OPTS", "-Xmx 512m"
            ));
        report.setSystemProperties(
            ImmutableMap.of(
                "user.home", "/users/jboss"
            ));
        report.setUser(
            ImmutableMap.of(
                "username", "joe.staffguy@cru.org",
                "guid", "ABE4234-FASFA",
                "employeeId", "000512345"
            ));
        report.setThrown(
            ImmutableList.<Throwable>of(
                new RuntimeException("something bad happened", boom())
            ));
        report.setContext(
            ImmutableMultimap.of(
                "framework", "resteasy-jaxrs-3.0.5.Final",
                "framework", "weld-core-1.1.16",
                "conferenceId", "10912"
            ));

        report.setWebContext(buildSampleWebContext());

    }

    private NullPointerException boom()
    {
        return new NullPointerException();
    }

    private WebContext buildSampleWebContext() throws Exception
    {
        WebContext context = new WebContext();
        context.setComponent(TestResource.class.getMethod("doSomething", String.class));
        context.setEntityRepresentation("{\"color\": \"blue\", \"size\":12}");
        context.setStart(new DateTime(2013, 12, 13, 14, 23, 37, 493));
        context.setFinish(new DateTime(2013, 12, 13, 14, 23, 37, 724));
        context.setHeaders(ImmutableMultimap.of(
            "Accept", "application/json",
            "Content-Type", "application/json",
            "Content-Encoding", "gzip",
            "X-Proxy", "proxy.somewhere.org",
            "X-Proxy", "proxy.somewhereelse.org"
        ));

        context.setMethod("PUT");
        context.setQueryParameters(ImmutableMultimap.of(
            "id", "2362134",
            "lang", "en_au",
            "lang", "en"
        ));
        //In real life, a request won't have both post parameters and an entity representation.
        context.setPostParameters(
            ImmutableMultimap.of(
                "real_life", "false",
                "credit_card_number", "<removed>"
            ));
        context.setResponseStatus(500);
        context.setUrl(new URI("https://api.tests.example.org/v1/conferences/ucf-fall-retreat/sessions"));

        return context;
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class TestResource
    {
        public void doSomething(String params)
        {
        }
    }



    private void checkXmlContent()
    {
        String xml = writer.toString();

        System.out.println(xml.replace("><", ">\n<"));

        assertThat(xml, startsWith("<?xml "));
        assertThat(xml, containsString("<notice version=\"2.4\">"));
        assertThat(xml, containsString("<api-key>secrets</api-key>"));
        assertThat(xml, containsString("<class>java.lang.RuntimeException</class>"));
        assertThat(xml, containsString("<line method=\"caused by: java.lang.NullPointerException\">"));
        assertThat(xml, containsString("<message>something bad happened</message>"));
        assertThat(xml, containsString("<component>TestResource</component>"));
        assertThat(xml, containsString("<action>doSomething(String)</action>"));
        assertThat(xml, containsString("<var key=\"lang\">[en_au, en]</var>"));
        assertThat(xml, containsString("<var key=\"context:conferenceId\">10912</var>"));
        assertThat(xml, containsString("<var key=\"env:JAVA_OPTS\">-Xmx 512m</var>"));
        assertThat(xml, containsString("<var key=\"HTTP_X_PROXY\">[proxy.somewhere.org, proxy.somewhereelse.org]</var>"));
        assertThat(xml, containsString("<var key=\"Server Hostname\">testserver.cru.org</var>"));
        assertThat(xml, containsString("<guid>ABE4234-FASFA</guid>"));
        assertThat(xml, containsString("<framework>[resteasy-jaxrs-3.0.5.Final, weld-core-1.1.16]</framework>"));
        assertThat(xml, endsWith("</notice>"));

    }
}
