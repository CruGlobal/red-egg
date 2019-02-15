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

        public static class CustomParameterSanitizer implements ParameterSanitizer
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
All parameters are removed by default.


Similarly, make an entity sanitizer.
This is unnecessary if your application only uses `ServletRequest.getParameter()` (and friends),
but if your application consumes anything else (such as json or xml),
or if your application does its own `x-www-form-urlencoded` parsing,
then you should consider creating one.

It could look like this:

        public static class CustomEntitySanitizer implements EntitySanitizer
        {
            @Override
            public String sanitizeEntity(String entityRepresentation)
            {
                return entityRepresentation.replaceAll(
                    "(\"secret\"\\s*:\\s*\")[^\"]+(\")",
                    "$1<removed>$2"
                );
            }
        }

This example removes the string value from a json key/value pair whose key is 'secret'.
Note: you will probably need to think carefully about how you implement this,
and you should have several test cases.

If you do not provide an entity sanitizer,
entities will be completely removed.




If your app sometimes needs to directly consume the request's http entity,
configure or write a request matcher to identify which requests must have access to them.
(For these requests, Red Egg will not call `ServletRequest.getParameter()` or related methods,
which have the side-effect of making
`ServletRequest.getInputStream()` and `ServletRequest.getReader()` unusable.)
You can implement your own `RequestMatcher` or use one of the built-in `RequestMatchers` factories:

    public class RedEggConfig
    {
        public
        @Produces
        @Default
        @EntityStreamPreservation
        RequestMatcher matcher = RequestMatchers.matchingPaths("/rest-api/.*");
    }



Make sure your exceptions are visible to Red Egg.  There's 3 ways (which can be combined) to accomplish this:

1. Log the exception with Log4j or with java.util.logging (or make sure your framework does so),
2. Throw the exception past the RedEgg filter, or
3. Inject the `WebErrorRecorder` into one of your interceptors and call `recordThrown()`.


If your web.xml version is 3.0 (or greater), you are done.

If your web.xml version is 2.5 (or less), add this to your web.xml:

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
        .setEntitySanitizer(new MyCustomEntitySanitizer()) // or use the built-in NoOpEntitySanitizer
        .setErrbitConfig(createConfig()) // see CDI section for example code
        .setEntityStreamPreservationMatcher(createMatcher()); // see CDI section for example code

Instead of injecting a `WebErrorRecorder`, look one up via the `RecorderFactory`.
You can get the `RecorderFactory` via `RedEgg.getRecorderFactory()`.
(The `RecorderFactory` is threadsafe and can be cached.)


Additional Context
==================

You can optionally record additional context information that will make your error reports more useful.
Inject a WebErrorRecorder into an appropriate class (likely an interceptor or decorator),
and use the `recordContext()` and/or `recordUser()` methods.

You may wish to record the JVM's environment variables; use `recordEnvironmentVariables()` for this.
They are not recorded automatically, since (at least in Docker apps) they often contain credentials.
You can, of course, sanitize the values before recording them, but Red Egg doesn't do this for you.

Likewise, System properties are not recorded automatically;
use `recordSystemProperties()` if you wish to.
(Again, you may want sanitize the properties before recording them.)

The hostname is recorded automatically, but you can override it with `recordLocalHost()`.


Error Details Links
===================

If an error occurs during a servlet request,
a Link header will be added to the http response with a link to the error details.
Downstream clients can use this in their logs and/or in their own error reports.
The Link uses an extension relation type called `org.cru.links:error-details`.

This project includes a JAX-RS client response filter that can be used to
extract and record this link automatically.
You will need to create a ClientResponseFilter instance
and register it with your ClientBuilder, Client, or WebTarget.


Building this project
=====================

This project requires maven.
A full build also requires that you specify an arquillian profile.
For command-line builds, generally this is a good route:

    mvn install -Parq-wildfly-managed

This will launch a wildfly instance, run the tests on it, and shut it down afterward.

If you are running tests from your IDE,
generally you will want to instead activate the `arq-wildfly-remote` profile.
This requires that you fire up wildfly yourself in a terminal window.
This will run the tests faster and give you quicker feedback as you develop.
Note: if you have run the `arq-wildfly-managed` profile recently,
you may need to run `mvn resources:testResources -Parq-wildfly-managed`
(maybe via the IDE) to ensure the IDE doesn't use a stale arquillian.xml.
