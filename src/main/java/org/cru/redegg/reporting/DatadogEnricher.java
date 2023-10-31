package org.cru.redegg.reporting;

import com.google.common.collect.Iterables;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;

public class DatadogEnricher
{
    private final Clock clock;

    public DatadogEnricher(Clock clock)
    {
        this.clock = clock;
    }

    @Inject
    public DatadogEnricher()
    {
        this.clock = Clock.systemDefaultZone();
    }

    public void enrich(ErrorReport report)
    {
        if (report.getContext().containsKey("dd.trace_id"))
        {
            final Collection<String> traceIds = report.getContext().get("dd.trace_id");
            if (traceIds.size() == 1)
            {
                String traceId = Iterables.getOnlyElement(traceIds);
                report.getContext().put("dd.trace.link", traceLink(traceId));
                final Instant start;
                final Instant finish;
                final WebContext webContext = report.getWebContext();
                if (webContext != null)
                {
                    start = webContext.getStart();
                    finish = webContext.getFinish();
                }
                else
                {
                    start = null;
                    finish = null;
                }
                report.getContext().put("dd.logs.link", logsLink(traceId, start, finish));
            }
        }
    }

    private Instant toInstant(ZonedDateTime date)
    {
        return date == null ? null : date.toInstant();
    }

    private String traceLink(final String traceId) {
        return String.format("https://app.datadoghq.com/apm/trace/%s", traceId);
    }

    private String logsLink(final String traceId, Instant start, Instant finish) {
        final ZonedDateTime dateTime = ZonedDateTime.now(clock);

        // If we don't know when the request started or finished
        // (or if there is no http request for this error),
        // we will just use a plus or minus 20 minute buffer to find log entries for this trace.
        // This is roughly consistent with what the datadog UI does,
        // when clicking on the logs link on a trace page.
        final long fromTimestampMillis = useOrDefault(start, dateTime.minusMinutes(20))
            .minusSeconds(30)
            .toEpochMilli();
        final long toTimestampMillis = useOrDefault(finish, dateTime.plusMinutes(20))
            .plusSeconds(30)
            .toEpochMilli();

        // I am not sure what these suffixes are, except maybe nanoseconds.
        // The rollbar UI seems to use four decimal places.
        final String fromTimestamp = fromTimestampMillis + ".0000";
        final String toTimestamp = toTimestampMillis + ".0000";

        // avoiding String.format() to avoid having to escape percent symbols
        return "https://app.datadoghq.com/logs?from_ts=" +
               fromTimestamp +
               "&index=main&live=false&query=trace_id%3A" +
               traceId +
               "&stream_sort=desc&to_ts=" +
               toTimestamp;
    }

    private Instant useOrDefault(Instant optionalInstant, ZonedDateTime defaultDateTime)
    {
        if (optionalInstant != null)
        {
            return optionalInstant;
        }
        else
        {
            return defaultDateTime.toInstant();
        }
    }

}
