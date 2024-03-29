package org.cru.redegg.reporting;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.cru.redegg.qualifier.Fallback;
import org.cru.redegg.reporting.api.ErrorLink;
import org.cru.redegg.reporting.api.ErrorReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Matt Drees
 */
@Fallback
public class LoggingReporter implements ErrorReporter
{
    private static final String NAME = "org.cru.redegg.loggedErrors";

    public static String name()
    {
        return NAME;
    }

    Logger errorLogger = LoggerFactory.getLogger(NAME);


    @Override
    public void send(ErrorReport report)
    {
        String message = buildMessage(report);

        if (report.getThrown().isEmpty())
        {
            errorLogger.error("error context:\n{}", message);
        }
        else
        {
            errorLogger.error("error:", report.getThrown().get(0));
            errorLogger.error("error context:\n{}", message);
        }
    }

    @Override
    public Optional<ErrorLink> buildLink()
    {
        return Optional.empty();
    }

    private String buildMessage(ErrorReport report)
    {
        ReportStringBuilder builder = new ReportStringBuilder();
        WebContext webContext = report.getWebContext();
        if (webContext != null)
        {
            addRequestInfo(webContext, builder);
        }
        if (report.getLocalHostName() != null)
        {
            builder.appendLine("Server", "%s (%s)", report.getLocalHostName(), report.getLocalHostAddress());
        }
        addMultimapEntries(builder, report.getContext(), "Other Context");
        addOtherThrownThrowables(report, builder);
        builder.appendChunk("User Information", report.getUser());
        builder.appendList("Logged Messages", report.getLogRecords());
        return builder.toString();
    }

    private void addOtherThrownThrowables(ErrorReport report, ReportStringBuilder builder)
    {
        List<Throwable> thrown = report.getThrown();
        if (thrown.size() > 1)
        {
            builder.appendNote("Other exceptions that were thrown:");
            for (int i = 1; i < thrown.size(); i++)
            {
                Throwable other = thrown.get(i);
                builder.appendChunk(String.valueOf(i), Throwables.getStackTraceAsString(other));
            }
        }
    }


    public void addRequestInfo(WebContext webContext, ReportStringBuilder builder)
    {
        builder.appendLine("request started", ISO_ZONED_DATE_TIME.format(webContext.getStart().atZone(ZoneId.systemDefault())));
        builder.appendLine("request finished", ISO_ZONED_DATE_TIME.format(webContext.getFinish().atZone(ZoneId.systemDefault())));
        builder.appendBreak();

        builder.appendLine("Method", webContext.getMethod());
        builder.appendLine("URL", webContext.getUrl());
        builder.appendLine("Query String", webContext.getQueryString());

        addMultimapEntries(builder, webContext.getQueryParameters(), "Query Parameters");
        addMultimapEntries(builder, webContext.getPostParameters(), "Post Parameters");
        addMultimapEntries(builder, webContext.getHeaders(), "Headers");
        if (webContext.getEntityRepresentation() != null)
        {
            builder.appendChunk("Request Entity", webContext.getEntityRepresentation());
        }
        builder.appendLine("Response Status", webContext.getResponseStatus());
    }

    private void addMultimapEntries(ReportStringBuilder builder, Multimap<String, String> multimap, String multimapName)
    {
        List<String> lines = Lists.newArrayList();
        for (Map.Entry<String, String> entry : multimap.entries())
        {
            lines.add(entry.getKey() + "=" + entry.getValue());
        }

        if (lines.isEmpty())
        {
            builder.appendNote("No " + multimapName);
        }
        else
        {
            builder.appendList(multimapName, lines);
        }
    }


}
