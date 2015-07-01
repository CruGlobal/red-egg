package org.cru.redegg.reporting;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.List;

public class DummyReportBuilder
{
    public ErrorReport buildDummyReport()
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
                TestExceptions.runtimeWrappingNullPointer()
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

}
