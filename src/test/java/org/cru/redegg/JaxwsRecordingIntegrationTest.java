package org.cru.redegg;


import org.cru.redegg.jaxws.RecordingSoapHandler;
import org.cru.redegg.recording.api.Action;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.reporting.LoggingReporter;
import org.cru.redegg.reporting.api.ErrorReporter;
import org.cru.redegg.test.AnswerWithSelf;
import org.cru.redegg.test.DefaultDeployment;
import org.cru.redegg.test.PortBuilder;
import org.cru.redegg.test.TestApplication;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.SOAPFaultException;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static javax.xml.soap.SOAPConstants.SOAP_1_2_PROTOCOL;
import static javax.xml.soap.SOAPConstants.SOAP_RECEIVER_FAULT;
import static javax.xml.soap.SOAPConstants.SOAP_SENDER_FAULT;
import static javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(Arquillian.class)
public class JaxwsRecordingIntegrationTest
{

    @Deployment
    public static WebArchive deployment()  {

        return DefaultDeployment.withCdi("jaxws-test.war")
            .addCoreWildflyPackages()
            .addRecordingConfigurationClasses()
            .getArchive()

            .addPackage(RecordingSoapHandler.class.getPackage())

            .addClass(LoggingReporter.class)
            .addClass(ErrorReporter.class)

            .addClass(TestApplication.class)
            .addClass(PortBuilder.class)
            .addClass(AnswerWithSelf.class)
            .addClass(RecordingMocks.class)

            .addAsWebInfResource("jaxws-web.xml", "web.xml")
            .addAsResource("org/cru/redegg/handlers.xml")
           ;
    }

    @Inject
    WebErrorRecorder recorder;

    @Inject
    RecordingMocks mocks;

    @Before
    public void setup()
    {
        mocks.reset();
    }

    @Test
    public void testSimpleJaxwsRequest() throws Exception {
        FruitService service = getFruitServiceClient();
        Fruit fruit = service.getFruit("red");

        assertThat(fruit.color, is(equalTo("red")));

        verify(recorder, atLeastOnce()).recordResponseStatus(200);

        verify(recorder).recordComponent(FruitServiceImpl.class.getMethod("getFruit", String.class));
    }

    @Test
    public void testJaxwsRequestClientFailure() throws Exception {
        FruitService service = getFruitServiceClient();
        try
        {
            service.getBadFruit(true);
        }
        catch (SOAPFaultException expected)
        {
            assertThat(expected.getMessage(), containsString("That's some bad fruit"));
            assertThat(expected.getFault().getFaultCodeAsQName(), equalTo(SOAP_SENDER_FAULT));
        }

        verify(recorder).recordResponseStatus(500);
        verify(recorder).userError();
    }

    @Test
    public void testJaxwsRequestServerFailure() throws Exception {
        FruitService service = getFruitServiceClient();
        try
        {
            service.getBadFruit(false);
        }
        catch (SOAPFaultException expected)
        {
            assertThat(expected.getMessage(), containsString("That's some bad fruit"));
            assertThat(expected.getFault().getFaultCodeAsQName(), equalTo(SOAP_RECEIVER_FAULT));
        }

        verify(recorder).recordResponseStatus(500);
        verify(recorder).error();
        verify(recorder, atLeastOnce()).recordLogRecord(argThat(isErrorLogRecord()));
        verify(recorder, never()).userError();
    }

    private Matcher<LogRecord> isErrorLogRecord()
    {
        return new TypeSafeDiagnosingMatcher<LogRecord>()
        {
            @Override
            protected boolean matchesSafely(
                LogRecord item, Description mismatchDescription)
            {
                if (item.getLevel().intValue() >= Level.SEVERE.intValue())
                    return true;
                else
                {
                    mismatchDescription.appendValue(item.getLevel());
                    return false;
                }
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText("SEVERE or higher");
            }
        };
    }


    private FruitService getFruitServiceClient()
    {
        return new PortBuilder().getPort(
            FruitService.class,
            serviceName,
            "jaxws-test",
            "FruitService?wsdl");
    }



    @XmlRootElement
    public static class Fruit
    {
        private String color;
        private double weight;

        public Fruit() {}

        public Fruit(String color, double weight)
        {
            this.color = color;
            this.weight = weight;
        }

        public String getColor()
        {
            return color;
        }

        public void setColor(String color)
        {
            this.color = color;
        }

        public double getWeight()
        {
            return weight;
        }

        public void setWeight(double weight)
        {
            this.weight = weight;
        }
    }

    private static final String namespace = "http://red-egg.cru.org/tests/jaxws";
    private static final QName serviceName = new QName(namespace, "FruitService");


    @WebService(
        targetNamespace = namespace
    )
    public static interface FruitService
    {

        @WebMethod
        public Fruit getFruit(@WebParam(name = "color") String color);

        @WebMethod
        public Fruit getBadFruit(@WebParam(name = "clientOrServer") boolean clientOrServer);
    }


    @SuppressWarnings("WSReferenceInspection") // intellij doesn't like the $ syntax, but CXF does
    @WebService(
        endpointInterface = "org.cru.redegg.JaxwsRecordingIntegrationTest$FruitService",
        serviceName = "FruitService",
        targetNamespace = namespace
    )
    @BindingType(value = SOAP12HTTP_BINDING)
    @HandlerChain(file = "handlers.xml")
    @Action
    public static class FruitServiceImpl implements FruitService
    {

        @Override
        public Fruit getFruit(String color)
        {
            return new Fruit(color, 2.3);
        }

        @Override
        public Fruit getBadFruit(boolean clientOrServer)
        {
            SOAPFault fault =
                createSoapFault(clientOrServer ? SOAP_SENDER_FAULT : SOAP_RECEIVER_FAULT);

            throw new SOAPFaultException(fault);
        }

        private SOAPFault createSoapFault(QName faultCode)
        {
            SOAPFault fault;
            try
            {
                SOAPFactory factory = SOAPFactory.newInstance(SOAP_1_2_PROTOCOL);
                fault = factory.createFault("That's some bad fruit", faultCode);
            }
            catch (SOAPException e)
            {
                throw new RuntimeException("unable to create SOAPFault!", e);
            }
            return fault;
        }


    }


}
