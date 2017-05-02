package org.cru.redegg.util;

import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

/**
 * @author Matt Drees
 */
public class SimplePomParser
{
    private final URL pomLocation;

    public SimplePomParser(URL pomLocation)
    {
        this.pomLocation = pomLocation;
    }

    public String getVersion()
    {
        XPathFactory xpathFactory =  XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        xpath.setNamespaceContext(new PomNamespaceContext());
        try
        {
            XPathExpression expression = xpath.compile("/pom:project/pom:version");

            InputStream stream = pomLocation.openStream();
            try
            {
                return expression.evaluate(new InputSource(stream));
            }
            finally
            {
                stream.close();
            }
        }
        catch (XPathExpressionException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    static class PomNamespaceContext implements NamespaceContext
    {

        public String getNamespaceURI(String prefix) {
            if (prefix == null) throw new NullPointerException("Null prefix");
            else if ("pom".equals(prefix)) return "http://maven.apache.org/POM/4.0.0";
            return XMLConstants.NULL_NS_URI;
        }

        // This method isn't necessary for XPath processing.
        public String getPrefix(String uri) {
            throw new UnsupportedOperationException();
        }

        // This method isn't necessary for XPath processing either.
        public Iterator<?> getPrefixes(String uri) {
            throw new UnsupportedOperationException();
        }
    }


}
