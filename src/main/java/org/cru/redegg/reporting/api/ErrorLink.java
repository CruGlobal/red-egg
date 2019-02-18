package org.cru.redegg.reporting.api;

import java.net.URI;

/**
 * Represents a link to a yet-to-be-created error details page.
 */
public interface ErrorLink
{
    /**
     * Returns a URI for a web page containing information on this error
     */
    URI getTarget();
}
