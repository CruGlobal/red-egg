package org.cru.redegg.jaxws;

import org.slf4j.Logger;
import org.cru.redegg.recording.api.ErrorRecorder;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Collections;
import java.util.Set;

import static javax.xml.soap.SOAPConstants.SOAP_SENDER_FAULT;
import static javax.xml.soap.SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE;

/**
 * A jax-ws {@link SOAPHandler} that records soap faults.
 * If the client is at fault (hah!), the error is recorded as a user error
 * (see {@link ErrorRecorder#userError()}).
 * This is determined by inspecting the soap fault code.
 *
 * The handler must be configured in a "handlers.xml" file. See
 * https://jax-ws.java.net/2.2.8/docs/ch03.html#section-165423693536683 .
 * The file should live in the same java package as the annotated service class.
 *
 * @author Matt Drees
 */
public class RecordingSoapHandler implements SOAPHandler<SOAPMessageContext>
{
    private Logger log = LoggerFactory.getLogger(getClass());

    /** SOAP 1.2 Client Fault */
    private static final QName SOAP_1_1_CLIENT_CODE = new QName(URI_NS_SOAP_1_1_ENVELOPE, "Client");

    //TODO: not sure we can rely on this working
    @Inject
    WebErrorRecorder recorder;


    @Override
    public Set<QName> getHeaders()
    {
        return Collections.emptySet();
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context)
    {
        return true; //continue processing the handler chain
    }

    @Override
    public boolean handleFault(SOAPMessageContext context)
    {
        SOAPBody soapBody = getSoapBody(context);
        if (soapBody != null)
        {
            recordAppropriateError(soapBody);
        }
        return true; //continue processing the handler chain
    }

    private SOAPBody getSoapBody(SOAPMessageContext context)
    {
        try
        {
            return context.getMessage().getSOAPBody();
        }
        catch (SOAPException e)
        {
            log.error("Unable to get soap body. This fault isn't being processed by red egg.", e);
            return null;
        }
    }

    private void recordAppropriateError(SOAPBody soapBody)
    {
        QName faultCode = soapBody.getFault().getFaultCodeAsQName();

        if (faultCode.equals(SOAP_1_1_CLIENT_CODE) ||
            faultCode.equals(SOAP_SENDER_FAULT))
        {
            recorder.userError();
        }
        else
        {
            recorder.error();
        }
    }

    @Override
    public void close(MessageContext context)
    {
    }
}
