A utility for Cru's java apps to use to send http details to Errbit.

The name comes from the name of the first
[Flight Data Recorder](http://en.wikipedia.org/wiki/Flight_data_recorder).


Usage (in an environment with CDI)
==================================
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

            // configure which classes will be treated as belonging to this app
            config.getApplicationBasePackages().addAll(
                ImmutableList.of("org.cru.yourapp"));

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

            @Override
            public List<String> sanitizeHeader(
                String headerName, List<String> headerValues)
            {
                return sanitize(headerName, headerValues);
            }

            private List<String> sanitize(String parameterName, List<String> parameterValues)
            {
                if (parameterName.equals("secret"))
                    return ImmutableList.of("<removed>");
                else
                    return parameterValues;
            }
        }
2. or use the NoOp sanitizer by 'producing' it in your config class, as such:

        public class RedEggConfig
        {
            //...

            @Produces ParameterSanitizer sanitizer = new NoOpParameterSanitizer();
        }

Make sure your exceptions are visible to Red Egg.  There's 3 ways (which can be combined) to accomplish this:

1. Log the exception with Log4j or with java.util.logging (or make sure your framework does so),
2. Throw the exception past the RedEgg filter, or
3. Inject the `WebErrorRecorder` into one of your interceptors and call `recordThrown()`.


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

Usage (in an environment without CDI)
=====================================

To use in an app that doesn't have CDI, or that doesn't support CDI injection in servlet listeners,
first do everything in the CDI section above.

In addition, add this to your `web.xml`:

    <context-param>
      <param-name>org.cru.redegg.no-cdi</param-name>
      <param-value>true</param-value>
    </context-param>

Instead of using @Producer methods,
configure a sanitizer and the errbit config via java code in your app's initialization code;
it should look like the following:

    RedEgg.configure()
        .setParameterSanitizer(new MyCustomParameterSanitizer()) // or use the built-in NoOpParameterSanitizer
        .setErrbitConfig(createConfig()); // see CDI section for example code

Instead of injecting a `WebErrorRecorder`, look one up via the `RecorderFactory`.
You can get the `RecorderFactory` via `RedEgg.getRecorderFactory()`.
(The `RecorderFactory` is threadsafe and can be cached.)


Additional Context
==================

You can optionally record additional context information that will make your error reports more useful.
Inject a WebErrorRecorder into an appropriate class (likely an interceptor or decorator),
and use the `recordContext()` and/or `recordUser()` methods.
