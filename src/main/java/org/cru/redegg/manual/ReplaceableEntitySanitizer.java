package org.cru.redegg.manual;

import org.cru.redegg.recording.api.EntitySanitizer;

/**
 * @author Matt Drees
 */
public class ReplaceableEntitySanitizer implements EntitySanitizer
{
    /* volatile needed because this could be changed by a different thread */
    private volatile EntitySanitizer delegate;

    public ReplaceableEntitySanitizer(EntitySanitizer delegate)
    {
        this.delegate = delegate;
    }

    public void replace(EntitySanitizer delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public String sanitizeEntity(String entityRepresentation)
    {
        return delegate.sanitizeEntity(entityRepresentation);
    }

}
