package org.cru.redegg.recording.gson;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.cru.redegg.recording.api.Serializer;

import java.util.Map;
import java.util.Set;

/**
 * @author Matt Drees
 */
public class GsonSerializer implements Serializer
{

    Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    @Override
    public String toString(Object value)
    {
        if (value instanceof String)
            return (String) value;
        else
            return gson.toJson(value);
    }

    @Override
    public Map<String, String> toStringMap(Object object)
    {
        JsonElement tree = gson.toJsonTree(object);
        if (tree instanceof JsonObject)
        {
            return buildMapFromEntries((JsonObject) tree);
        }
        else if (tree instanceof JsonPrimitive)
        {
            return ImmutableMap.of("id", tree.toString());
        }
        else if (tree instanceof JsonNull)
        {
            return ImmutableMap.of();
        }
        else
        {
            return ImmutableMap.of("data", tree.toString());
        }

    }

    private Map<String, String> buildMapFromEntries(JsonObject jsonObject)
    {
        Set<Map.Entry<String,JsonElement>> entries = jsonObject.entrySet();
        Map<String, String> stringMap = Maps.newHashMapWithExpectedSize(entries.size());
        for (Map.Entry<String, JsonElement> entry : entries)
        {
            stringMap.put(entry.getKey(), toString(entry.getValue()));
        }
        return stringMap;
    }
}
