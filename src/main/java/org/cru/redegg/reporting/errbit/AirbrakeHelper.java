package org.cru.redegg.reporting.errbit;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

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

    public Map<String, Object> getOtherWebContextDetails(WebContext webContext)
    {
        Map<String, Object> otherWebDetails = Maps.newHashMap();
        if (webContext.getStart() != null)
        {
            otherWebDetails.put("Request Started", formatter.print(webContext.getStart()));
        }
        if (webContext.getFinish() != null)
        {
            otherWebDetails.put("Request Finished", formatter.print(webContext.getFinish()));
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

    private final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();


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