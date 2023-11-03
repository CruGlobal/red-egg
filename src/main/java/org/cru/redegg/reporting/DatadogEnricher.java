package org.cru.redegg.reporting;

import com.google.common.collect.Iterables;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;

/**
 * Builds a useful link to datadog's Log Explorer UI, and adds it to the context.
 * Datadog's agent adds a special 'dd.trace_id' to the MDC for every log event,
 * which we can leverage here.
 * This also copies the trace id to the 'trace_id' address, because the rollbar docs recommend
 * this value.
 * Maybe someday it'll be useful in the Rollbar UI.
 */
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

                report.getContext().put("trace_id", traceId);

                report.getContext().put("trace_link", traceLink(traceId));
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
                report.getContext().put("dd_logs_link", logsLink(traceId, start, finish));
            }
        }
    }

    private Instant toInstant(ZonedDateTime date)
    {
        return date == null ? null : date.toInstant();
    }

    private String traceLink(final String traceId) {
        return String.format("https://app.datadoghq.com/apm/trace/%s?spanViewType=logs", traceId);
    }

    /**
     * Builds something like
     * https://app.datadoghq.com/logs?query=trace_id%3A4280747157143919621&from_ts=1698791125829&to_ts=1698793127886&live=false
     * which should redirect to something like
     * https://app.datadoghq.com/logs?query=trace_id%3A4280747157143919621&cols=%40http.url_details.path%2C%40network.client.ip&index=%2A&messageDisplay=inline&stream_sort=desc&viz=stream&from_ts=1698791125829&to_ts=1698793127886&live=false
     */
    private String logsLink(final String traceId, Instant start, Instant finish) {
        final ZonedDateTime dateTime = ZonedDateTime.now(clock);

        // If we don't know when the request started or finished
        // (or if there is no http request for this error),
        // we will just use a plus or minus 20 minute buffer to find log entries for this trace.
        // This is roughly consistent with what the datadog UI does,
        // when clicking on the logs link on a trace page.
        final long fromTimestamp = useOrDefault(start, dateTime.minusMinutes(20))
            .minusSeconds(30)
            .toEpochMilli();
        final long toTimestamp = useOrDefault(finish, dateTime.plusMinutes(20))
            .plusSeconds(30)
            .toEpochMilli();

        // avoiding String.format() to avoid having to escape percent symbols
        return "https://app.datadoghq.com/logs?" +
                "query=trace_id%3A" +
                traceId +
                "&from_ts=" +
                fromTimestamp +
                "&to_ts=" +
                toTimestamp +
                "&live=false";
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
