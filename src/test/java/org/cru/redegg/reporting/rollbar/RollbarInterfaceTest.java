package org.cru.redegg.reporting.rollbar;

import org.cru.redegg.reporting.DummyReportBuilder;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.util.RedEggVersion;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Matt Drees
 */
public class RollbarInterfaceTest
{
    public static void main(String[] args)
    {
        new RollbarInterfaceTest().run(args[0]);
    }

    private void run(String key)
    {
        RollbarReporter reporter = configReporter(key);
        ErrorReport report = new DummyReportBuilder().buildDummyReport();
        reporter.send(report);
    }

    private RollbarReporter configReporter(String accessToken)
    {
        RollbarConfig config = new RollbarConfig();
        config.setAccessToken(accessToken);
        config.setEnvironmentName("interface-test");
        config.setBranch("add-rollbar-support");
        config.setIdentifyingUserProperty("guid");
        config.setCodeVersion(RedEggVersion.get());
        return new RollbarReporter(config, new HardCodedFileNameResolver());
    }


    private static class HardCodedFileNameResolver implements FileNameResolver
    {
        @Override
        public String addPath(String fileName, String className)
        {
            if (className.startsWith("org.cru.redegg.reporting.rollbar"))
                return "org/cru/redegg/reporting/rollbar/" + fileName;
            else if (className.startsWith("org.cru.redegg.reporting"))
                return "org/cru/redegg/reporting/" + fileName;
            else
                return fileName;
        }
    }
}
