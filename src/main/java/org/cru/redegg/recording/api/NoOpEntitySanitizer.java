package org.cru.redegg.recording.api;

import javax.enterprise.inject.Alternative;

/**
 * Performs no sanitizing at all.  Parameters are returned as-is.
 *
 * @author Matt Drees
 */
@Alternative
public class NoOpEntitySanitizer implements EntitySanitizer
{
    @Override
    public String sanitizeEntity(String entityRepresentation)
    {
        return entityRepresentation;
    }
}
