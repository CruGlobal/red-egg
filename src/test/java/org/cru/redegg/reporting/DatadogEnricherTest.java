package org.cru.redegg.reporting;

import com.google.common.collect.HashMultimap;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class DatadogEnricherTest
{

    ErrorReport report = new ErrorReport();

    private final ZoneOffset testZone = ZoneOffset.UTC;
    private final Instant testNow = LocalDate.of(2018, 2, 14)
        .atStartOfDay()
        .toInstant(testZone);

    Clock clock = Clock.fixed(testNow, testZone);


    @Test
    public void testNormalWebRequest()
    {
        final WebContext webContext = new WebContext();
        webContext.setStart(testNow.minusSeconds(10));
        webContext.setFinish(testNow.minusSeconds(6));
        report.addWebContext(webContext);
        final HashMultimap<String, String> context = HashMultimap.create();
        context.put("dd.trace_id", "9219263634118187777");
        report.setContext(context);

        new DatadogEnricher(clock).enrich(report);
        checkSpanLink();
        assertThat(report.getContext().get("dd_logs_link"), is(equalTo(singleton("https://app.datadoghq.com/logs?query=trace_id%3A9219263634118187777&from_ts=1518566360000&to_ts=1518566424000&live=false"))));
    }

    @Test
    public void testUnfinishedWebRequest()
    {
        final WebContext webContext = new WebContext();
        webContext.setStart(testNow.minusSeconds(10));
        report.addWebContext(webContext);
        final HashMultimap<String, String> context = HashMultimap.create();
        context.put("dd.trace_id", "9219263634118187777");
        report.setContext(context);

        new DatadogEnricher(clock).enrich(report);
        checkSpanLink();
        assertThat(report.getContext().get("dd_logs_link"), is(equalTo(singleton("https://app.datadoghq.com/logs?query=trace_id%3A9219263634118187777&from_ts=1518566360000&to_ts=1518567630000&live=false"))));
    }

    @Test
    public void testNonWebRequest()
    {
        final HashMultimap<String, String> context = HashMultimap.create();
        context.put("dd.trace_id", "9219263634118187777");
        report.setContext(context);

        new DatadogEnricher(clock).enrich(report);
        checkSpanLink();
        assertThat(report.getContext().get("dd_logs_link"), is(equalTo(singleton("https://app.datadoghq.com/logs?query=trace_id%3A9219263634118187777&from_ts=1518565170000&to_ts=1518567630000&live=false"))));
    }

    private void checkSpanLink() {
        assertThat(report.getContext().get("trace_link"), is(equalTo(singleton("https://app.datadoghq.com/apm/trace/9219263634118187777?spanViewType=logs"))));
    }
}
