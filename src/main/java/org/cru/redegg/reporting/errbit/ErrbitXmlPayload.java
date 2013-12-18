package org.cru.redegg.reporting.errbit;

import com.google.common.collect.Multimap;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.cru.redegg.util.RedEggCollections;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import static org.cru.redegg.util.RedEggCollections.flatten;

/**
 * @author Matt Drees
 */
public class ErrbitXmlPayload
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

    public void writeXmlTo(Writer underlyingWriter)
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
        writeElementWithContent("version", "1-SNAPSHOT"); //TODO: programmatically drive this
        writeElementWithContent("url", "https://github.com/CruGlobal/red-egg");
        writer.writeEndElement();
    }

    private void writeFrameworkIfPossible() throws XMLStreamException
    {
        Multimap<String, String> context = report.getContext();
        if (context.containsKey("framework"))
        {
            writeElementWithContent("framework", flatten(context).get("framework").toString());
        }
    }

    private void writeError() throws XMLStreamException
    {
        writer.writeStartElement("error");
        writeExceptionClassIfPossible();
        writeMessageIfPossible();
        writeBacktrace();
        writer.writeEndElement();
    }

    private void writeExceptionClassIfPossible() throws XMLStreamException
    {
        if (!report.getThrown().isEmpty())
        {
            writeElementWithContent("class", report.getThrown().get(0).getClass().getName());
        }
    }

    private void writeMessageIfPossible() throws XMLStreamException
    {
        if (report.getRootErrorMessage().isPresent())
        {
            writeElementWithContent("message", report.getRootErrorMessage().get());
        }
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
        writeStackTraceElements(stackTrace, stackTrace.length - 1);
        Throwable cause = throwable.getCause();
        if (cause != null)
        {
            writeBacktraceForCause(cause, stackTrace);
        }
    }

    private void writeBacktraceForCause(Throwable cause, StackTraceElement[] causedTrace) throws XMLStreamException
    {
        StackTraceElement[] causeTrace = cause.getStackTrace();
        int smallestCommonFrame = determineIndexOfSmallestCommonFrame(causedTrace, causeTrace);
        int framesInCommon = causeTrace.length - 1 - smallestCommonFrame;

        writeBacktraceLineMessage("caused by: " + cause.toString());
        writeStackTraceElements(cause.getStackTrace(), smallestCommonFrame);
        if (framesInCommon != 0)
            writeBacktraceLineMessage("    ... " + framesInCommon + " more");
    }


    private int determineIndexOfSmallestCommonFrame(StackTraceElement[] causedTrace, StackTraceElement[] causeTrace)
    {
        int causeIndex = causeTrace.length - 1;
        int causedIndex = causedTrace.length - 1;
        while (indicesValid(causeIndex, causedIndex) &&
               causeTrace[causeIndex].equals(causedTrace[causedIndex]))
        {
            causeIndex--;
            causedIndex--;
        }
        return causeIndex;
    }

    private void writeStackTraceElements(StackTraceElement[] stackTrace, int maxIndexToWrite) throws XMLStreamException
    {
        if (stackTrace != null)
        {
            for (int i=0; i <= maxIndexToWrite; i++)
            {
                writeLine(stackTrace[i]);
            }
        }
    }

    private boolean indicesValid(int causeIndex, int causedIndex)
    {
        return causeIndex >= 0 && causedIndex >= 0;
    }


    //TODO: is this the right way to communicate 'non-frame' information?
    private void writeBacktraceLineMessage(String message) throws XMLStreamException
    {
        writer.writeStartElement("line");
        writer.writeAttribute("method", message);
        writer.writeEndElement();
    }

    private void writeLine(StackTraceElement element) throws XMLStreamException
    {
        writer.writeStartElement("line");
        int lineNumber = element.getLineNumber();
        if (lineNumber > 0)
            writer.writeAttribute("number", String.valueOf(lineNumber));
        String fileName = element.getFileName();
        if (fileName != null)
            writer.writeAttribute("file", fileName);
        String method = element.getClassName() + '.' + element.getMethodName();
        writer.writeAttribute("method", method);
        writer.writeEndElement();
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
        Map<String, Object> cgiVariables = helper.toCgiVariables(RedEggCollections.flatten(webContext.getHeaders()));
        writeVariables(cgiVariables);
        writeVariables(helper.getOtherWebContextDetails(webContext));
        writeOtherUsefulInformation();
        writer.writeEndElement();
    }

    // there doesn't seem to be a have a better place for this.
    // we'll stick it in the request cgi-data block
    private void writeOtherUsefulInformation() throws XMLStreamException
    {
        writeVariables(helper.prefixKeys("context:", RedEggCollections.flatten(report.getContext())));
        writeVariables(helper.prefixKeys("environment:", report.getEnvironmentVariables()));
        writeVariables(helper.prefixKeys("system-property:", report.getSystemProperties()));
        writeVariables(helper.getDetails(report));
    }

    private void writeVariables(Map<String, ?> cgiVariables) throws XMLStreamException
    {
        for (Map.Entry<String, ?> entry : cgiVariables.entrySet())
        {
            writeVar(entry.getKey(), entry.getValue().toString());
        }
    }

    private void writeParameters(WebContext webContext) throws XMLStreamException
    {
        writer.writeStartElement("params");
        Multimap<String, String> requestParameters = webContext.getCombinedQueryAndPostParameters();
        if (!requestParameters.isEmpty())
        {
            for (Map.Entry<String, Object> entry : flatten(requestParameters).entrySet())
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
        if (webContext.getComponent() != null)
        {
            Class<?> declaringClass = webContext.getComponent().getDeclaringClass();
            writeElementWithContent("component", declaringClass.getSimpleName());
            String methodName = webContext.getComponent().toGenericString();
            writeElementWithContent("action", methodName.replace(declaringClass.getName() + ".", ""));
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
