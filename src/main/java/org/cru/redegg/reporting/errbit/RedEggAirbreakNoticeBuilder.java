package org.cru.redegg.reporting.errbit;

import airbrake.AirbrakeNoticeBuilder;
import airbrake.Backtrace;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Matt Drees
 */
public class RedEggAirbreakNoticeBuilder extends AirbrakeNoticeBuilder
{

    public static RedEggAirbreakNoticeBuilder build(ErrbitConfig config, ErrorReport report)
    {
        if (!report.getThrown().isEmpty())
        {
            return new RedEggAirbreakNoticeBuilder(config, report, report.getThrown().get(0));
        }
        else if (!report.getLogRecords().isEmpty())
        {
            return new RedEggAirbreakNoticeBuilder(config, report, report.getLogRecords().get(0));
        }
        else
        {
            return new RedEggAirbreakNoticeBuilder(config, report, "error (message unavailable)");
        }
    }

    private RedEggAirbreakNoticeBuilder(ErrbitConfig config, ErrorReport report, String errorMessage)
    {
        super(config.getKey(), errorMessage, config.getEnvironmentName());
        addContext(report);
    }

    private RedEggAirbreakNoticeBuilder(ErrbitConfig config, ErrorReport report, Throwable rootError)
    {
        super(config.getKey(), builder, rootError, config.getEnvironmentName());
        assert rootError == report.getThrown().get(0);
        addContext(report);
    }

    private void addContext(ErrorReport report)
    {
        standardEnvironmentFilters();
        ec2EnvironmentFilters();

        WebContext webContext = report.getWebContext();
        if (webContext != null)
        {
            String component = webContext.getComponent() == null ? null : webContext.getComponent().toString();
            setRequest(webContext.getUrl().toString(), component);
            Multimap<String, String> combined = getCombinedQueryAndPostParameters(webContext);
            request(getAsStringMapIfPossible(combined));
            environment(toCgiVariables(getAsStringMapIfPossible(webContext.getHeaders())));
            environment(getOtherWebContextDetails(webContext));
        }

        //TODO: use errbit's user-attributes section for this.  Requires patching or ditching the Airbreak-Java lib.
        environment(prefixKeys("user:", report.getUser()));

        environment(prefixKeys("context:", getAsStringMapIfPossible(report.getContext())));
        environment(prefixKeys("environment:", report.getEnvironmentVariables()));
        environment(prefixKeys("system-property:", report.getSystemProperties()));
        environment(getDetails(report));
    }

    private Map<String, Object> toCgiVariables(Map<String, Object> headerMap)
    {
        Map<String, Object> cgiVariables = Maps.newHashMapWithExpectedSize(headerMap.size());
        for (Map.Entry<String, Object> entry : headerMap.entrySet())
        {
            cgiVariables.put(toCgiVariableName(entry.getKey()), entry.getValue());
        }
        return cgiVariables;
    }

    private String toCgiVariableName(String headerName)
    {
        return "HTTP_" + headerName.toUpperCase().replace('-', '_');
    }

    private Map<String, Object> getDetails(ErrorReport report)
    {
        Map<String, Object> otherDetails = Maps.newHashMap();
        if (report.getLocalHostName() != null)
        {
            otherDetails.put("Server Hostname", report.getLocalHostName());
        }
        if (report.getLocalHostAddress() != null)
        {
            otherDetails.put("Server IP Address", report.getLocalHostAddress());
        }
        if (!report.getLogRecords().isEmpty())
        {
            String logSnippet = Joiner.on("\n").join(report.getLogRecords());
            otherDetails.put("Log Snippet", logSnippet);
        }
        if (report.getThrown().size() > 1)
        {
            List<String> otherStackTraces = convertOtherStacktraces(report);
            otherDetails.put("Other Thrown Exceptions", Joiner.on("\n\n").join(otherStackTraces));
        }
        return otherDetails;
    }

    private List<String> convertOtherStacktraces(ErrorReport report)
    {
        List<String> otherStackTraces = Lists.newArrayListWithCapacity(report.getThrown().size() - 1);
        for (int i = 1; i < report.getThrown().size(); i++)
        {
            Throwable other = report.getThrown().get(i);
            otherStackTraces.add(Throwables.getStackTraceAsString(other));
        }
        return otherStackTraces;
    }

    private Multimap<String, String> getCombinedQueryAndPostParameters(WebContext webContext)
    {
        Multimap<String, String> combined =
            HashMultimap.create(webContext.getQueryParameters().size() + webContext.getPostParameters().size(), 1);
        combined.putAll(webContext.getQueryParameters());
        combined.putAll(webContext.getPostParameters());
        return combined;
    }

    private Map<String, Object> getOtherWebContextDetails(WebContext webContext)
    {
        Map<String, Object> otherWebDetails = Maps.newHashMap();
        if (webContext.getStart() != null)
        {
            otherWebDetails.put("request started", formatter.print(webContext.getStart()));
        }
        if (webContext.getFinish() != null)
        {
            otherWebDetails.put("request finished", formatter.print(webContext.getFinish()));
        }
        if (webContext.getMethod() != null)
        {
            otherWebDetails.put("REQUEST_METHOD", webContext.getMethod());
        }
        if (webContext.getEntityRepresentation() != null)
        {
            otherWebDetails.put("http entity", webContext.getEntityRepresentation());
        }
        if (webContext.getResponseStatus() != null)
        {
            otherWebDetails.put("response status code", webContext.getResponseStatus());
        }

        //errbit depends on the cgi environment variable HTTP_USER_AGENT to do browser detection;
        Collection<String> userAgent = webContext.getHeaders().get("User-Agent");
        if (!userAgent.isEmpty())
        {
            otherWebDetails.put("HTTP_USER_AGENT", userAgent.iterator().next());
        }

        return otherWebDetails;
    }

    private final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();

    private Map<String, Object> prefixKeys(String prefix, Map<String, ?> map)
    {
        Map<String, Object> prefixed = Maps.newHashMapWithExpectedSize(map.size());
        for (Map.Entry<String, ?> entry : map.entrySet())
        {
            prefixed.put(prefix + entry.getKey(), entry.getValue());
        }
        return prefixed;
    }

    private Map<String, Object> getAsStringMapIfPossible(Multimap<String, String> multimap)
    {
        return flatten(multimap.asMap());
    }

    private Map<String, Object> flatten(Map<String, Collection<String>> map)
    {
        Map<String, Object> flattened = Maps.newHashMapWithExpectedSize(map.size());
        for (Map.Entry<String, Collection<String>> entry : map.entrySet())
        {
            Object newValue;
            if (entry.getValue().size() == 1)
                newValue = Iterables.getOnlyElement(entry.getValue());
            else
                newValue = entry.getValue();
            flattened.put(entry.getKey(), newValue);
        }
        return flattened;
    }

    public static class RedEggBacktrace extends Backtrace
    {

    }
    public static final Backtrace builder = new RedEggBacktrace();
}
