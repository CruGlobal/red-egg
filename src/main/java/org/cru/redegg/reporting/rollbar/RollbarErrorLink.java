package org.cru.redegg.reporting.rollbar;

import org.cru.redegg.reporting.api.ErrorLink;

import java.net.URI;
import java.util.UUID;

/**
 * Contains a rollbar item UUID and renders it as a rollbar item link.
 */
public class RollbarErrorLink implements ErrorLink
{
    private final UUID id;

    public RollbarErrorLink(UUID id)
    {
        this.id = id;
    }

    @Override
    public URI getTarget()
    {
        return URI.create("https://rollbar.com/occurrence/uuid/?uuid=" + id);
    }

    public UUID getId()
    {
        return id;
    }
}
