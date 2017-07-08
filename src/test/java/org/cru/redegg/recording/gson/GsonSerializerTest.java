package org.cru.redegg.recording.gson;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Matt Drees
 */
public class GsonSerializerTest
{


    private GsonSerializer gsonSerializer = new GsonSerializer();

    class TestUser {

        public Long id;
        public String username;
        public String email;
    }


    @Test
    public void testSerializeToMap() {
        TestUser user = new TestUser();
        user.id = 3L;
        user.username = "roger";
        user.email = "roger@example.com";


        Map<String, String> stringStringMap = gsonSerializer.toStringMap(user);

        Map<String, String> expected = ImmutableMap.of(
            "id", "3",
            "username", "roger",
            "email", "roger@example.com"
        );
        assertThat(stringStringMap, Matchers.is(Matchers.equalTo(expected)));
    }
}