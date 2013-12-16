package org.cru.redegg.reporting.errbit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.joda.time.DateTime;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * @author Matt Drees
 */
public class ErrbitInterfaceTest
{
    public static void main(String[] args)
    {
        new ErrbitInterfaceTest().run(args[0], args[1]);
    }

    private void run(String endpoint, String key)
    {
        NativeErrbitReporter reporter = configReporter(endpoint, key);
        ErrorReport report = buildDummyReport();
        reporter.send(report);
    }

    private NativeErrbitReporter configReporter(String endpoint, String key)
    {
        ErrbitConfig config = new ErrbitConfig();
        config.setKey(key);
        config.setEnvironmentName("interface-test");
        try
        {
            config.setEndpoint(new URL(endpoint));
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
        return new NativeErrbitReporter(config);
    }


    private ErrorReport buildDummyReport()
    {
        ErrorReport report = new ErrorReport();
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

        try
        {
            report.setWebContext(buildSampleWebContext());
        }
        catch (Exception e)
        {
            throw new AssertionError(e);
        }
        return report;
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
        context.setStart(new DateTime(2013, 12, 13, 14, 23, 37, 724));
        context.setFinish(new DateTime(2013, 12, 13, 14, 23, 37, 493));
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

}
