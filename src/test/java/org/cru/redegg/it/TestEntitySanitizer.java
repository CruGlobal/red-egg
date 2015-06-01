package org.cru.redegg.it;

import org.cru.redegg.recording.api.EntitySanitizer;

/**
 * @author Matt Drees
 */
public class TestEntitySanitizer implements EntitySanitizer
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
