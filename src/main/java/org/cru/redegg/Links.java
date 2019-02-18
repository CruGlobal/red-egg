package org.cru.redegg;

public class Links
{
    private Links() {}

    /**
     * A private-use (https://tools.ietf.org/html/bcp35#section-3.8) URI
     * that serves as an extension relation type (https://tools.ietf.org/html/rfc8288#section-2.1.2).
     * A link of this type refers to a location where more details can be found regarding
     * the error that caused this request's processing to fail (or degrade).
     */
    public static final String CRU_ERROR_DETAILS_REL_TYPE = "org.cru.links:error-details";

}
