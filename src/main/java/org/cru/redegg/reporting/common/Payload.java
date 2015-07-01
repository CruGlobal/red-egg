package org.cru.redegg.reporting.common;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Matt Drees
 */
public interface Payload
{

    void writeTo(Writer writer) throws IOException;
}
