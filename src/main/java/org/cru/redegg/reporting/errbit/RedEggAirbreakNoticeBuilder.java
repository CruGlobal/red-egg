package org.cru.redegg.reporting.errbit;

import airbrake.AirbrakeNoticeBuilder;
import airbrake.Backtrace;
import org.cru.redegg.reporting.ErrorReport;
import org.cru.redegg.reporting.WebContext;
import org.cru.redegg.util.RedEggCollections;

/**
 * @author Matt Drees
 */
public class RedEggAirbreakNoticeBuilder extends AirbrakeNoticeBuilder
{

    private final AirbrakeHelper airbrakeHelper = new AirbrakeHelper();

    public static RedEggAirbreakNoticeBuilder build(ErrbitConfig config, ErrorReport report)
    {
        if (report.getRootException().isPresent())
        {
            return new RedEggAirbreakNoticeBuilder(config, report, report.getRootException().get());
        }
        else if (report.getRootErrorMessage().isPresent())
        {
            return new RedEggAirbreakNoticeBuilder(config, report, report.getRootErrorMessage().get());
        }
        else
        {
            return new RedEggAirbreakNoticeBuilder(config, report, "error (message unavailable)");
        }
    }

    private RedEggAirbreakNoticeBuilder(ErrbitConfig config, ErrorReport report, String errorMessage)
    {
        super(config.getKey(), errorMessage, config.getEnvironmentName());
        addContext(report);
    }

    private RedEggAirbreakNoticeBuilder(ErrbitConfig config, ErrorReport report, Throwable rootError)
    {
        super(config.getKey(), builder, rootError, config.getEnvironmentName());
        assert rootError == report.getThrown().get(0);
        addContext(report);
    }

    private void addContext(ErrorReport report)
    {
        standardEnvironmentFilters();
        ec2EnvironmentFilters();

        WebContext webContext = report.getWebContext();
        if (webContext != null)
        {
            String component = webContext.getComponent() == null ? null : webContext.getComponent().toString();
            setRequest(webContext.getUrl().toString(), component);
            request(RedEggCollections.flatten(webContext.getCombinedQueryAndPostParameters()));
            environment(airbrakeHelper.toCgiVariables(RedEggCollections.flatten(webContext.getHeaders())));
            environment(airbrakeHelper.getOtherWebContextDetails(webContext));
        }

        //TODO: use errbit's user-attributes section for this.  Requires patching or ditching the Airbreak-Java lib.
        environment(airbrakeHelper.prefixKeys("user:", report.getUser()));

        environment(airbrakeHelper.prefixKeys("context:", RedEggCollections.flatten(report.getContext())));
        environment(airbrakeHelper.prefixKeys("environment:", report.getEnvironmentVariables()));
        environment(airbrakeHelper.prefixKeys("system-property:", report.getSystemProperties()));
        environment(airbrakeHelper.getDetails(report));
    }


    public static class RedEggBacktrace extends Backtrace
    {

    }
    public static final Backtrace builder = new RedEggBacktrace();
}
