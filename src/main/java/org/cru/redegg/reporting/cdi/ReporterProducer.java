package org.cru.redegg.reporting.cdi;

import org.cru.redegg.qualifier.Fallback;
import org.cru.redegg.qualifier.Selected;
import org.cru.redegg.reporting.api.ErrorReporter;
import org.cru.redegg.reporting.errbit.ErrbitConfig;
import org.cru.redegg.reporting.errbit.NativeErrbitReporter;
import org.cru.redegg.reporting.rollbar.RollbarConfig;
import org.cru.redegg.reporting.rollbar.RollbarReporter;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

/**
 * @author Matt Drees
 */
public class ReporterProducer
{
    public
    @Produces
    @Selected
    ErrorReporter selectErrorReporter(
        Instance<ErrbitConfig> errbitConfig,
        Instance<NativeErrbitReporter> errbitReporter,
        Instance<RollbarConfig> rollbarConfig,
        Instance<RollbarReporter> rollbarReporter,
        @Fallback ErrorReporter fallbackReporter)
    {
        if (available(errbitConfig))
            return new NativeErrbitReporter(errbitConfig.get());
        else if (available(rollbarConfig))
            return new RollbarReporter(rollbarConfig.get());
        else
            return fallbackReporter;
    }

    private boolean available(Instance<?> instance)
    {
        return !instance.isUnsatisfied();
    }

}
