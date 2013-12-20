package org.cru.redegg.reporting.errbit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.joda.time.DateTime;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * @author Matt Drees
 */
public class ErrbitInterfaceTest
{
    public static void main(String[] args)
    {
        new ErrbitInterfaceTest().run(args[0], args[1]);
    }

    private void run(String endpoint, String key)
    {
        NativeErrbitReporter reporter = configReporter(endpoint, key);
        ErrorReport report = new DummyReportBuilder().buildDummyReport();
        reporter.send(report);
    }

    private NativeErrbitReporter configReporter(String endpoint, String key)
    {
        ErrbitConfig config = new ErrbitConfig();
        config.setKey(key);
        config.setEnvironmentName("interface-test");
        config.getApplicationBasePackages().add("org.cru.redegg");
        config.setSourcePrefix("src/test/java");
        try
        {
            config.setEndpoint(new URI(endpoint));
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
        return new NativeErrbitReporter(config);
    }


}
