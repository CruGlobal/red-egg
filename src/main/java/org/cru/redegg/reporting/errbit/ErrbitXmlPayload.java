package org.cru.redegg.reporting.errbit;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.cru.redegg.reporting.common.Payload;
import org.cru.redegg.reporting.common.Reporters;
import org.cru.redegg.util.RedEggCollections;
import org.cru.redegg.util.RedEggVersion;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.cru.redegg.util.RedEggStrings.truncate;

/**
 * @author Matt Drees
 */
public class ErrbitXmlPayload implements Payload
{
    private ErrorReport report;
    private ErrbitConfig config;
    private XMLStreamWriter writer;

    private AirbrakeHelper helper = new AirbrakeHelper();

    public ErrbitXmlPayload(ErrorReport report, ErrbitConfig config)
    {
        this.report = report;
        this.config = config;
    }

    public void writeTo(Writer underlyingWriter)
    {
        try
        {
            writer = XMLOutputFactory.newInstance().createXMLStreamWriter(underlyingWriter);
        } catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
        try
        {
            toXmlInternal();
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void toXmlInternal() throws XMLStreamException
    {
        writer.writeStartDocument();
        writer.writeStartElement("notice");
        writer.writeAttribute("version", "2.4");
        writeNoticeContent();
        writer.writeEndElement();
        writer.writeEndDocument();
    }

    private void writeNoticeContent() throws XMLStreamException
    {
        writeApiKey();
        writeNotifier();
        writeFrameworkIfPossible();
        writeError();
        writeRequest();
        writeServerEnvironment();
        writeCurrentUser();
    }

    private void writeApiKey() throws XMLStreamException
    {
        writeElementWithContent("api-key", config.getKey());
    }

    private void writeNotifier() throws XMLStreamException
    {
        writer.writeStartElement("notifier");
        writeElementWithContent("name", "red-egg");
        writeElementWithContent("version", RedEggVersion.get());
        writeElementWithContent("url", "https://github.com/CruGlobal/red-egg");
        writer.writeEndElement();
    }

    private void writeFrameworkIfPossible() throws XMLStreamException
    {
        Multimap<String, String> context = report.getContext();
        if (context.containsKey("framework"))
        {
            writeElementWithContent("framework", RedEggCollections.flatten(context).get("framework").toString());
        }
    }

    private void writeError() throws XMLStreamException
    {
        writer.writeStartElement("error");
        writeExceptionClassIfPossible();
        writeMessage();
        writeBacktrace();
        writer.writeEndElement();
    }

    private void writeExceptionClassIfPossible() throws XMLStreamException
    {
        if (!report.getThrown().isEmpty())
        {
            writeElementWithContent("class", report.getRootException().get().getClass().getName());
        }
    }

    private void writeMessage() throws XMLStreamException
    {
        writeElementWithContent("message", report.getRootErrorMessage().or("(message not available)"));
    }

    private void writeBacktrace() throws XMLStreamException
    {
        writer.writeStartElement("backtrace");
        if (report.getRootException().isPresent())
            writeBacktraceFor(report.getRootException().get());
        writer.writeEndElement();
    }

    private void writeBacktraceFor(Throwable throwable) throws XMLStreamException
    {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace != null)
        {
            for (StackTraceElement element : stackTrace)
            {
                writeLine(element);
            }
        }
    }

    private void writeLine(StackTraceElement element) throws XMLStreamException
    {
        writer.writeStartElement("line");
        int lineNumber = element.getLineNumber();
        if (lineNumber > 0)
            writer.writeAttribute("number", String.valueOf(lineNumber));
        String fileName = element.getFileName();
        if (fileName != null)
            writer.writeAttribute("file", decorate(fileName, element.getClassName()));
        String method = element.getClassName() + '.' + element.getMethodName();
        writer.writeAttribute("method", normalize(method));
        writer.writeEndElement();
    }

    /**
     * Reflection is very commonly used, and Sun/Oracle's implementation uses generated classes whose names can vary.
     * To keep this from causing different Errbit fingerprints, we normalize the generated class names.
     */
    private String normalize(String method)
    {
        return SUN_REFLECTION_PATTERN.matcher(method).replaceFirst("$1_N_");
    }

    private static final Pattern SUN_REFLECTION_PATTERN =
        Pattern.compile("(sun\\.reflect\\.GeneratedMethodAccessor)\\d+");


    //Add path information, if it appears to be a class from this application.
    //This causes Errbit to render a link to the source file instead of using plain text.
    private String decorate(String fileName, String className)
    {
        String packageName = determinePackageName(className);
        if (packageName == null)
            return fileName;
        for (String basePackage : config.getApplicationBasePackages())
        {
            if (packageName.startsWith(basePackage) && notGenerated(fileName))
                return "[PROJECT_ROOT]/" + prefix() + toPath(packageName) + fileName;
        }
        return fileName;
    }

    private boolean notGenerated(String fileName)
    {
        //most generated classes use a dollar sign in the 'pretend' filename, I believe
        return !fileName.contains("$");
    }

    private String toPath(String packageName)
    {
        return packageName.replace('.', '/') + "/";
    }

    private String determinePackageName(String className)
    {
        try
        {
            Package classPackage = Class.forName(className).getPackage();
            return classPackage == null ? null : classPackage.getName();
        }
        catch (ClassNotFoundException ignored)
        // not all classes on the stack may be visible to this class's classloader
        {
            return guessPackageFromDots(className);
        }
    }

    private String guessPackageFromDots(String className)
    {
        int index = className.lastIndexOf('.');
        if (index == -1)
            return null;
        return className.substring(0, index);
    }

    private String prefix()
    {
        String prefix = config.getSourcePrefix();
        if (prefix.isEmpty() || prefix.endsWith("/"))
            return prefix;
        else
            return prefix + "/";
    }

    private void writeRequest() throws XMLStreamException
    {
        writer.writeStartElement("request");
        WebContext webContext = report.getWebContext();
        if (webContext != null)
        {
            writeRequestInformation(webContext);
        }
        else
        {
            writeNonRequestInformation();
        }
        writer.writeEndElement();
    }

    private void writeNonRequestInformation() throws XMLStreamException
    {
        writer.writeStartElement("cgi-data");
        writeOtherUsefulInformation();
        writer.writeEndElement();
    }

    private void writeRequestInformation(WebContext webContext) throws XMLStreamException
    {
        writeUrl(webContext);
        writeComponentIfPossible(webContext);
        writeParameters(webContext);
        writeCgiVariables(webContext);
    }

    /*
        note: this doesn't allow for nested variables.
        Errbit's test suite seems to indicate it can handle them.
        Not sure if it's worth implementing here.
     */
    private void writeCgiVariables(WebContext webContext) throws XMLStreamException
    {
        writer.writeStartElement("cgi-data");
        Map<String, Object> flattenedHeaders = RedEggCollections.flatten(webContext.getHeaders());
        writeVariables(helper.toCgiVariables(flattenedHeaders));
        writeVariables(helper.getOtherWebContextDetails(webContext));
        writeOtherUsefulInformation();
        writer.writeEndElement();
    }

    // there doesn't seem to be a have a better place for this.
    // we'll stick it in the request cgi-data block
    private void writeOtherUsefulInformation() throws XMLStreamException
    {
        writeVariables(helper.getDetails(report));
        writeVariables(helper.prefixKeys("context:", RedEggCollections.flatten(report.getContext())));
        // the prefixes are shortened because the errbit display has limited width,
        // and the key column is too wide to be practical
        writeVariables(helper.prefixKeys("env:", report.getEnvironmentVariables()));
        writeVariables(helper.prefixKeys("sysprop:", report.getSystemProperties()));
    }

    private void writeVariables(Map<String, ?> cgiVariables) throws XMLStreamException
    {
        for (Map.Entry<String, ?> entry : cgiVariables.entrySet())
        {
            writeVar(truncate(entry.getKey(), 35, "..."), entry.getValue().toString());
        }
    }


    private void writeParameters(WebContext webContext) throws XMLStreamException
    {
        writer.writeStartElement("params");
        Multimap<String, String> requestParameters = webContext.getCombinedQueryAndPostParameters();
        if (!requestParameters.isEmpty())
        {
            for (Map.Entry<String, Object> entry : RedEggCollections.flatten(requestParameters).entrySet())
            {
                writeVar(entry.getKey(), entry.getValue().toString());
            }
        }
        writer.writeEndElement();
    }

    private void writeVar(String key, String value) throws XMLStreamException
    {
        writer.writeStartElement("var");
        writer.writeAttribute("key", key);
        writer.writeCharacters(value);
        writer.writeEndElement();
    }

    private void writeUrl(WebContext webContext) throws XMLStreamException
    {
        writer.writeStartElement("url");
        writer.writeCData(webContext.getUrl().toString());
        writer.writeEndElement();
    }

    private void writeComponentIfPossible(WebContext webContext) throws XMLStreamException
    {
        Method component = webContext.getComponent();
        if (component != null)
        {
            Class<?> declaringClass = component.getDeclaringClass();
            writeElementWithContent("component", declaringClass.getSimpleName());
            writeElementWithContent("action", Reporters.buildSimplifiedMethodName(component));
        }
    }

    private void writeServerEnvironment() throws XMLStreamException
    {
        writer.writeStartElement("server-environment");
        //TODO: handle project-root if possible
        writeElementWithContent("project-root", "");
        writeElementWithContent("environment-name", config.getEnvironmentName());
        writer.writeEndElement();
    }

    private void writeCurrentUser() throws XMLStreamException
    {
        writer.writeStartElement("current-user");
        for (Map.Entry<String, String> entry : report.getUser().entrySet())
        {
            writeElementWithContent(entry.getKey(), entry.getValue());
        }
        writer.writeEndElement();
    }

    private void writeElementWithContent(String elementName, String content) throws XMLStreamException
    {
        writer.writeStartElement(elementName);
        writer.writeCharacters(content);
        writer.writeEndElement();
    }
}
