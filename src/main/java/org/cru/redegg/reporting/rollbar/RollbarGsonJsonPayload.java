package org.cru.redegg.reporting.rollbar;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gson.stream.JsonWriter;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.cru.redegg.reporting.common.Payload;
import org.cru.redegg.reporting.common.Reporters;
import org.cru.redegg.util.RedEggCollections;
import org.cru.redegg.util.RedEggStrings;
import org.cru.redegg.util.RedEggVersion;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.cru.redegg.recording.api.NotificationLevel.WARNING;

/**
 *
 * @author Matt Drees
 */
public class RollbarGsonJsonPayload implements Payload
{
    RollbarConfig config;
    ErrorReport report;
    FileNameResolver resolver;

    JsonWriter writer;

    boolean prettyPrint;

    public RollbarGsonJsonPayload(ErrorReport report, RollbarConfig config, FileNameResolver resolver)
    {
        this.report = report;
        this.config = config;
        this.resolver = resolver;
    }

    public RollbarGsonJsonPayload prettyPrint()
    {
        prettyPrint = true;
        return this;
    }

    public void writeTo(Writer underlyingWriter) throws IOException
    {
        writer = new JsonWriter(underlyingWriter);
        if (prettyPrint)
        {
            writer.setIndent("  ");
        }
        writeJson();
        writer.close();
    }

    private void writeJson() throws IOException
    {
        writer.beginObject();
        writeAccessToken();
        writeData();
        writer.endObject();
    }

    private void writeAccessToken() throws IOException
    {
        writer.name("access_token").value(config.getAccessToken());
    }

    private void writeData() throws IOException
    {
        writer.name("data");
        writer.beginObject();
        writeEnvironment();
        writeBody();

        if (report.getNotificationLevel() == WARNING)
        {
            writer.name("level").value("warning");
        }

        //TODO: timestamp

        writer.name("code_version").value(config.getCodeVersion());
        writer.name("platform").value(config.getPlatform());
        writer.name("language").value("java");

        if (report.getContext().containsKey("framework"))
        {
            String framework =
                RedEggCollections.flatten(report.getContext()).get("framework").toString();
            writer.name("framework").value(framework);
        }

        if (report.getWebContext() != null)
        {
            writeWebContext();
        }

        writePerson();
        writeServer();
        writeCustom();
        writeNotifier();

        writer.endObject();
    }

    private void writeBody() throws IOException
    {
        writer.name("body");
        writer.beginObject();

        if (report.getThrown().isEmpty())
        {
            writeMessage();
        }
        else
        {
            writeTraceChain(report.getThrown().get(0));
        }

        writer.endObject();
    }

    private void writeMessage() throws IOException
    {
        writer.name("message");
        writer.beginObject();
        writer.name("body").value(report.getRootErrorMessage().or("(message not available)"));
        writer.endObject();
    }

    private void writeTraceChain(Throwable throwable) throws IOException
    {
        writer.name("trace_chain");
        writeTraceChainArray(throwable);
    }

    private void writeEnvironment() throws IOException
    {
        writer.name("environment").value(config.getEnvironmentName());
    }

    private void writeWebContext() throws IOException
    {
        writeContext();
        writeRequest();
    }

    private void writeContext() throws IOException
    {
        Method component = report.getWebContext().getComponent();
        String context =
            component.getDeclaringClass().getSimpleName() +
            "." + // or '#" ?
            Reporters.buildSimplifiedMethodName(component);
        writer.name("context").value(context);
    }

    private void writeRequest() throws IOException
    {
        writer.name("request");
        writer.beginObject();
        WebContext webContext = report.getWebContext();
        writeUrl(webContext);
        writeMethod(webContext);
        writeHeaders(webContext);
        writeQueryStringParameters(webContext);
        writePostParameters(webContext);
        writeBody(webContext);

        //TODO:
        //writeUserIp();

        writeStart(webContext);
        writeFinish(webContext);
        writeResponseCode(webContext);

        writer.endObject();

    }

    private void writeUrl(WebContext webContext) throws IOException
    {
        writer.name("url").value(webContext.getUrl().toString());
    }

    private void writeMethod(WebContext webContext) throws IOException
    {
        writer.name("method").value(webContext.getMethod());
    }

    private void writeHeaders(WebContext webContext) throws IOException
    {
        writer.name("headers");
        writeMultimapWithValuesSeparatedByCommas(webContext.getHeaders());
    }

    private void writeQueryStringParameters(WebContext webContext) throws IOException
    {
        writer.name("GET");
        writeMultimapWithValuesSeparatedByCommas(webContext.getQueryParameters());
    }

    private void writePostParameters(WebContext webContext) throws IOException
    {
        writer.name("POST");
        writeMultimapWithValuesSeparatedByCommas(webContext.getPostParameters());
    }

    private void writeMultimapWithValuesSeparatedByCommas(Multimap<String, String> multimap)
        throws IOException
    {
        writer.beginObject();
        for (Map.Entry<String, Collection<String>> entry : multimap.asMap().entrySet())
        {
            writer.name(entry.getKey());
            writer.value(Joiner.on(",").join(entry.getValue()));
        }
        writer.endObject();
    }

    private void writeBody(WebContext webContext) throws IOException
    {
        writer.name("body").value(webContext.getEntityRepresentation());
    }

    private void writeStart(WebContext webContext) throws IOException
    {
        writer.name("start").value(webContext.getStart().toString());
    }

    private void writeFinish(WebContext webContext) throws IOException
    {
        writer.name("finish").value(webContext.getFinish().toString());
    }

    private void writeResponseCode(WebContext webContext) throws IOException
    {
        writer.name("response_status_code").value(webContext.getResponseStatus().toString());
    }

    private void writePerson() throws IOException
    {
        Map<String, String> user = report.getUser();
        if (user.isEmpty())
            return;

        writer.name("person");
        writer.beginObject();


        writer.name("id");
        writer.value(determineId(user));

        for (Map.Entry<String, String> entry : user.entrySet())
        {
            if (!entry.getKey().equals("id"))
            {
                writer.name(entry.getKey());
                writer.value(entry.getValue());
            }
        }
        writer.endObject();
    }

    private String determineId(Map<String, String> user)
    {
        String idProperty = config.getIdentifyingUserProperty();
        if (idProperty != null)
        {
            String id = user.get(idProperty);
            if (id != null)
                return truncateId(id);
        }

        // TODO: log warning?

        String idGuess = user.entrySet().iterator().next().getValue();
        return truncateId(idGuess);
    }

    private String truncateId(String id)
    {
        return RedEggStrings.truncate(id, 40, "");
    }


    private void writeServer() throws IOException
    {
        writer.name("server");
        writer.beginObject();

        writeHost();
        writeBranch();

        writeSystemProperties();
        writeEnvironmentVariables();

        writer.endObject();
    }

    private void writeHost() throws IOException
    {
        writer.name("host");
        writer.value(report.getLocalHostName());

        //TODO: not sure this is recorded by rollbar
        writer.name("hostAddress");
        writer.value(report.getLocalHostAddress());
    }

    private void writeBranch() throws IOException
    {
        String branch = config.getBranch();
        if (branch != null)
        {
            writer.name("branch");
            writer.value(branch);
        }
    }

    private void writeSystemProperties() throws IOException
    {
        writer.name("system_properties");
        writeMap(report.getSystemProperties());
    }

    private void writeEnvironmentVariables() throws IOException
    {
        writer.name("environment_variables");
        writeMap(report.getEnvironmentVariables());
    }

    private void writeMap(Map<String, String> systemProperties) throws IOException
    {
        writer.beginObject();
        for (Map.Entry<String, String> entry : systemProperties.entrySet())
        {
            writer.name(entry.getKey());
            writer.value(entry.getValue());
        }
        writer.endObject();
    }

    private void writeNotifier() throws IOException
    {
        writer.name("notifier");
        writer.beginObject();

        writer.name("name").value("red-egg");
        writer.name("version").value(RedEggVersion.get());

        //TODO: maybe not?
        writer.name("url").value("https://github.com/CruGlobal/red-egg");

        writer.endObject();
    }

    private void writeCustom() throws IOException
    {
        writer.name("custom");
        writer.beginObject();

        writeExtraContext();
        writeExtraThrowables();
        writeLogMessages();

        writer.endObject();
    }

    private void writeExtraContext() throws IOException
    {
        writer.name("context");
        writeMultimapWithValuesAsArraysIfNecessary(report.getContext());
    }

    private void writeMultimapWithValuesAsArraysIfNecessary(Multimap<String, String> multimap)
        throws IOException
    {
        writer.beginObject();
        for (String key : multimap.keys())
        {
            writer.name(key);

            Collection<String> values = multimap.get(key);
            if (values.size() > 1)
            {
                writeArray(values);
            }
            else
            {
                writer.value(Iterables.getOnlyElement(values));
            }
        }
        writer.endObject();

    }

    private void writeArray(Collection<String> values) throws IOException
    {
        writer.beginArray();
        for (String value : values)
        {
            writer.value(value);
        }
        writer.endArray();
    }


    private void writeExtraThrowables() throws IOException
    {
        List<Throwable> thrown = report.getThrown();
        if (thrown.size() > 1)
        {
            writer.name("other_trace_chains");
            writer.beginArray();
            for (int i = 1; i < thrown.size(); i++)
            {
                writeTraceChainArray(thrown.get(i));
            }
            writer.endArray();
        }
    }

    private void writeTraceChainArray(Throwable throwable) throws IOException
    {
        writer.beginArray();
        List<Throwable> chain = Throwables.getCausalChain(throwable);
        for (Throwable link : Lists.reverse(chain))
        {
            writeTrace(link);
        }
        writer.endArray();
    }

    private void writeTrace(Throwable link) throws IOException
    {
        writer.beginObject();
        writeFrames(link);
        writeException(link);
        writer.endObject();
    }

    private void writeFrames(Throwable link) throws IOException
    {
        writer.name("frames");
        writer.beginArray();
        List<StackTraceElement> reversed = Lists.reverse(Arrays.asList(link.getStackTrace()));
        for (StackTraceElement element : reversed)
        {
            writeFrame(element);
        }
        writer.endArray();
    }

    private void writeFrame(StackTraceElement element) throws IOException
    {
        writer.beginObject();

        // TODO: this doesn't include the full path, as docs require.
        String className = element.getClassName();
        String fileName = element.getFileName();
        if (fileName != null)
        {
            writer.name("filename").value(resolver.addPath(fileName, className));
        }

        if (element.getLineNumber() >= 0)
        {
            writer.name("lineno").value(element.getLineNumber());
        }

        String method = className + '.' + element.getMethodName();
        writer.name("method").value(method);

        // it'd be nice to provide code, context, and args,
        // but that is difficult to do in java.

        writer.endObject();
    }

    private void writeException(Throwable link) throws IOException
    {
        writer.name("exception");
        writer.beginObject();
        writer.name("class").value(link.getClass().getName());
        writer.name("message").value(link.getMessage());

        // TODO: implement this somehow?
        //writer.name("description").value(?);

        writer.endObject();
    }

    private void writeLogMessages() throws IOException
    {
        writer.name("log_messages");
        writer.beginArray();
        List<String> logRecords = report.getLogRecords();
        for (String logRecord : logRecords)
        {
            writer.value(logRecord);
        }
        writer.endArray();
    }
}
