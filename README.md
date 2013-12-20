A utility for Cru's java apps to use to send http details to Errbit.

The name comes from the name of the first
[Flight Data Recorder](http://en.wikipedia.org/wiki/Flight_data_recorder).


Usage
=====
Include the maven dependency:

    <dependency>
        <groupId>org.ccci</groupId>
        <artifactId>red-egg</artifactId>
        <version>1-SNAPSHOT</version>
    </dependency>

Make an `ErrbitConfig` producer.  For example:

    public class RedEggConfig
    {
        public
        @Produces ErrbitConfig createConfig() throws URISyntaxException
        {
            ErrbitConfig config = new ErrbitConfig();
            config.setEndpoint(new URI("https://errors.uscm.org/notifier_api/v2/notices"));
            config.setKey("paste-your-app's-key-here");
            config.setEnvironmentName(determineEnvironmentName());
            return config;
        }

        private String determineEnvironmentName()
        {
            // use a System property or something to drive this
        }
    }

Make a parameter sanitizer. This will keep sensitive data out of the error database.  You have 2 options:

1. Create a custom sanitizer class yourself:

        public static class CustomSanitizer implements ParameterSanitizer
        {

            @Override
            public List<String> sanitizeQueryStringParameter(
                String parameterName, List<String> parameterValues)
            {
                return sanitize(parameterName, parameterValues);
            }

            @Override
            public List<String> sanitizePostBodyParameter(
                String parameterName, List<String> parameterValues)
            {
                return sanitize(parameterName, parameterValues);
            }

            private List<String> sanitize(String parameterName, List<String> parameterValues)
            {
                if (parameterName.equals("secret"))
                    return ImmutableList.of("<removed>");
                else
                    return parameterValues;
            }
        }
2. or use the NoOp sanitizer by adding this to your `<alternatives>` list in beans.xml:

        <alternatives>
            <class>org.cru.redegg.recording.api.NoOpParameterSanitizer</class>
        </alternatives>


Make sure your exceptions are visible to Red Egg.  There's 3 ways (which can be combined) to accomplish this:

1. Log the exception with Log4j or with java.util.logging (or make sure your framework does so),
2. Throw the exception past the RedEgg filter, or
3. Inject the WebErrorRecorder into one of your interceptors and call `recordThrown()`.


If you are using a Servlet 3 (or greater) servlet container, you are done.

If you are using a Servlet 2.5 (or less) servlet container, add this to your web.xml:

    <filter>
        <filter-name>RedEggFilter</filter-name>
        <filter-class>org.cru.redegg.servlet.RedEggFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>RedEggFilter</filter-name>
        <!-- generally you will want to map to all urls -->
        <url-pattern>/*</url-pattern>
    </filter-mapping>

    <listener>
        <listener-class>org.cru.redegg.servlet.RedEggServletListener</listener-class>
    </listener>

Note: This project currently requires a CDI environment.
Servlet filters & listeners need CDI injection capability.
In non-java-ee environments, this requires implementation-specific configuration
(for example, see
[Weld's servlet integration](http://docs.jboss.org/weld/reference/1.1.16.Final/en-US/html/environments.html#d0e5228)).

Eventually I'd like CDI to be optional, since not all Cru apps use CDI (yet).


You can optionally record additional context information that will make your error reports more useful.
Inject a WebErrorRecorder into an appropriate class (likely an interceptor or decorator),
and use the `recordContext()` and/or `recordUser()` methods.
