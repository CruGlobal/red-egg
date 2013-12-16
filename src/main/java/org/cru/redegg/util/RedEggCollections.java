package org.cru.redegg.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Map;

/**
 * @author Matt Drees
 */
public class RedEggCollections
{
    public static Map<String, Object> flatten(Multimap<String, ?> multimap)
    {
        Map<String, ? extends Collection<?>> map = multimap.asMap();
        Map<String, Object> flattened = Maps.newHashMapWithExpectedSize(map.size());
        for (Map.Entry<String, ? extends Collection<?>> entry : map.entrySet())
        {
            Object newValue;
            if (entry.getValue().size() == 1)
                newValue = Iterables.getOnlyElement(entry.getValue());
            else
                newValue = entry.getValue();
            flattened.put(entry.getKey(), newValue);
        }
        return flattened;
    }
}
