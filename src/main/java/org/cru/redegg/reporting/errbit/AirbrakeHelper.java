package org.cru.redegg.reporting.errbit;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.ZoneId;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AirbrakeHelper
{

    Map<String, Object> toCgiVariables(Map<String, Object> headerMap)
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

    Map<String, Object> getDetails(ErrorReport report)
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
        if (moreThanOneException(report))
        {
            List<String> stackTraces = convertStacktraces(report);
            otherDetails.put("All Thrown Exceptions", Joiner.on("\n\n").join(stackTraces));
        }
        return otherDetails;
    }

    private boolean moreThanOneException(ErrorReport report)
    {
        List<Throwable> thrown = report.getThrown();
        return thrown.size() > 1 ||
               (thrown.size() == 1 &&
                thrown.get(0).getCause() != null);
    }

    private List<String> convertStacktraces(ErrorReport report)
    {
        List<String> stackTraces = Lists.newArrayListWithCapacity(report.getThrown().size());
        for (Throwable throwable : report.getThrown())
        {
            stackTraces.add(Throwables.getStackTraceAsString(throwable));
        }
        return stackTraces;
    }

    public Map<String, Object> getOtherWebContextDetails(WebContext webContext)
    {
        Map<String, Object> otherWebDetails = Maps.newHashMap();
        if (webContext.getStart() != null)
        {
            otherWebDetails.put("Request Started", ISO_ZONED_DATE_TIME.format(webContext.getStart().atZone(ZoneId.systemDefault())));
        }
        if (webContext.getFinish() != null)
        {
            otherWebDetails.put("Request Finished", ISO_ZONED_DATE_TIME.format(webContext.getFinish().atZone(ZoneId.systemDefault())));
        }
        if (webContext.getMethod() != null)
        {
            otherWebDetails.put("REQUEST_METHOD", webContext.getMethod());
        }
        if (webContext.getEntityRepresentation() != null)
        {
            otherWebDetails.put("Http Entity", webContext.getEntityRepresentation());
        }
        if (webContext.getResponseStatus() != null)
        {
            otherWebDetails.put("Response Status Code", webContext.getResponseStatus());
        }

        return otherWebDetails;
    }


    Map<String, Object> prefixKeys(String prefix, Map<String, ?> map)
    {
        Map<String, Object> prefixed = new LinkedHashMap<String, Object>(map.size());
        for (Map.Entry<String, ?> entry : map.entrySet())
        {
            prefixed.put(prefix + entry.getKey(), entry.getValue());
        }
        return prefixed;
    }
}
