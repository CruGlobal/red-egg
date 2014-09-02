package org.cru.redegg.recording.api;

import org.cru.redegg.manual.Builder;
import org.cru.redegg.reporting.errbit.ErrbitConfig;

/**
 * If you are not using CDI, use this class to configure Red Egg and to obtain a RecorderFactory
 *
 * @author Matt Drees
 */
public class RedEgg
{
    private Builder builder = Builder.getInstance();

    private RedEgg () {}

    public static RecorderFactory getRecorderFactory()
    {
        return Builder.getInstance().getRecorderFactory();
    }


    public static RedEgg configure()
    {
        return new RedEgg();
    }

    public RedEgg setErrbitConfig(ErrbitConfig config)
    {
        config.validate();
        builder.setErrbitConfig(config);
        return this;
    }

    public RedEgg setParameterSanitizer(ParameterSanitizer sanitizer)
    {
        builder.setParameterSanitizer(sanitizer);
        return this;
    }
}
