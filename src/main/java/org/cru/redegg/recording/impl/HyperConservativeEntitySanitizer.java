package org.cru.redegg.recording.impl;

import org.cru.redegg.qualifier.Fallback;
import org.cru.redegg.recording.api.EntitySanitizer;

import java.util.logging.Logger;

/**
 * An entity sanitizer that filters out everything.
 *
 * This is the entity sanitizer used if the user does not configure one.
 *
 * @author Matt Drees
 */
@Fallback
public class HyperConservativeEntitySanitizer implements EntitySanitizer
{
    private static boolean warned;

    @Override
    public String sanitizeEntity(String entityRepresentation)
    {
        warnIfNecessary();
        return "<removed>";
    }

    private void warnIfNecessary()
    {
        if (!warned)
        {
            warned = true;
            Logger.getLogger(getClass().getName()).warning(
                "removing entity for this request." +
                " To avoid this message or to gather more detail," +
                " configure a less conservative strategy");
        }
    }
}
