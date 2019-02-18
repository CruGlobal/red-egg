package org.cru.redegg.reporting;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import org.joda.time.DateTime;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class DatadogEnricherTest
{

    ErrorReport report = new ErrorReport();

    private final ZoneOffset testZone = ZoneOffset.UTC;
    private Instant testNow = LocalDate.of(2018, 2, 14)
        .atStartOfDay()
        .toInstant(testZone);

    Clock clock = Clock.fixed(testNow, testZone);


    @Test
    public void testNormalWebRequest()
    {
        final WebContext webContext = new WebContext();
        webContext.setStart(new DateTime(Date.from(testNow.minusSeconds(10))));
        webContext.setFinish(new DateTime(Date.from(testNow.minusSeconds(6))));
        report.addWebContext(webContext);
        final HashMultimap<String, String> context = HashMultimap.create();
        context.put("dd.trace_id", "9219263634118187777");
        report.setContext(context);

        new DatadogEnricher(clock).enrich(report);
        assertThat(report.getContext().get("dd.trace.link"), is(equalTo(singleton("https://app.datadoghq.com/apm/trace/9219263634118187777"))));
        assertThat(report.getContext().get("dd.logs.link"), is(equalTo(singleton("https://app.datadoghq.com/logs?from_ts=1518566360000.0000&index=main&live=false&query=trace_id%3A9219263634118187777&stream_sort=desc&to_ts=1518566424000.0000"))));
    }

    @Test
    public void testUnfinishedWebRequest()
    {
        final WebContext webContext = new WebContext();
        webContext.setStart(new DateTime(Date.from(testNow.minusSeconds(10))));
        report.addWebContext(webContext);
        final HashMultimap<String, String> context = HashMultimap.create();
        context.put("dd.trace_id", "9219263634118187777");
        report.setContext(context);

        new DatadogEnricher(clock).enrich(report);
        assertThat(report.getContext().get("dd.trace.link"), is(equalTo(singleton("https://app.datadoghq.com/apm/trace/9219263634118187777"))));
        assertThat(report.getContext().get("dd.logs.link"), is(equalTo(singleton("https://app.datadoghq.com/logs?from_ts=1518566360000.0000&index=main&live=false&query=trace_id%3A9219263634118187777&stream_sort=desc&to_ts=1518567630000.0000"))));
    }

    @Test
    public void testNonWebRequest()
    {
        final HashMultimap<String, String> context = HashMultimap.create();
        context.put("dd.trace_id", "9219263634118187777");
        report.setContext(context);

        new DatadogEnricher(clock).enrich(report);
        assertThat(report.getContext().get("dd.trace.link"), is(equalTo(singleton("https://app.datadoghq.com/apm/trace/9219263634118187777"))));
        assertThat(report.getContext().get("dd.logs.link"), is(equalTo(singleton("https://app.datadoghq.com/logs?from_ts=1518565170000.0000&index=main&live=false&query=trace_id%3A9219263634118187777&stream_sort=desc&to_ts=1518567630000.0000"))));
    }
}