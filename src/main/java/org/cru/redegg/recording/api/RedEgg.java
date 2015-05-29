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

    /**
     * Enables Errbit reporting.
     *
     * At minimum, the Errbit url and the api key must be present.
     * If this method is not called, red-egg will simply log failures.
     */
    public RedEgg setErrbitConfig(ErrbitConfig config)
    {
        config.validate();
        builder.setErrbitConfig(config);
        return this;
    }

    /**
     * Configures a custom parameter sanitizer.
     *
     * If this method is not called, all parameters will be removed.
     */
    public RedEgg setParameterSanitizer(ParameterSanitizer sanitizer)
    {
        builder.setParameterSanitizer(sanitizer);
        return this;
    }

    /**
     * Configures a custom entity sanitizer.
     *
     * If this method is not called, the entity will be completely removed.
     */
    public RedEgg setEntitySanitizer(EntitySanitizer sanitizer)
    {
        builder.setEntitySanitizer(sanitizer);
        return this;
    }
}
