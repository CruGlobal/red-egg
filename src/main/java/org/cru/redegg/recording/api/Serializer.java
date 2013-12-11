package org.cru.redegg.recording.api;

import java.util.Map;

/**
 * @author Matt Drees
 */
public interface Serializer
{
    String toString(Object value);

    Map<String,String> toStringMap(Object object);
}
