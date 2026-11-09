/*
 * Copyright (C) 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.pustike.json;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.json.JsonValue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for collection and array serialization/deserialization via {@link ObjectMapper}.
 */
public class CollectionsTest {

    private final ObjectMapper mapper = new ObjectMapper(new TypeConverter());

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void toJsonValue_list() {
        List<String> list = List.of("a", "b", "c");
        JsonValue json = mapper.toJsonValue(list);
        assertEquals(JsonValue.ValueType.ARRAY, json.getValueType());
    }

    @Test
    public void readValue_listOfMaps() {
        // Deserializing to List with Object elements returns maps for JSON objects
        String json = "[{\"name\":\"a\"},{\"name\":\"b\"}]";
        List<?> list = mapper.readValue(json, List.class);
        assertEquals(2, list.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) list.getFirst();
        assertEquals("a", first.get("name"));
    }

    @Test
    public void toJsonValue_array() {
        String[] arr = new String[]{"a", "b", "c"};
        JsonValue json = mapper.toJsonValue(arr);
        assertEquals(JsonValue.ValueType.ARRAY, json.getValueType());
    }

    @Test
    public void toJsonValue_set() {
        Set<String> set = Set.of("a", "b", "c");
        JsonValue json = mapper.toJsonValue(set);
        assertEquals(jakarta.json.JsonValue.ValueType.ARRAY, json.getValueType());
    }

    @Test
    public void readValue_listOfPojo_viaTypeArguments() {
        // When deserializing a JSON array to List.class, elements become Maps
        String json = "[{\"name\":\"a\",\"age\":1},{\"name\":\"b\",\"age\":2}]";
        List<?> list = mapper.readValue(json, List.class);
        assertEquals(2, list.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) list.getFirst();
        assertEquals("a", first.get("name"));
        assertEquals(1, first.get("age"));
    }

    @Test
    public void toJsonValue_listOfPojo() {
        List<TestPojo.SimplePojo> list = new ArrayList<>();
        TestPojo.SimplePojo p1 = new TestPojo.SimplePojo();
        p1.setName("a");
        p1.setAge(1);
        TestPojo.SimplePojo p2 = new TestPojo.SimplePojo();
        p2.setName("b");
        p2.setAge(2);
        list.add(p1);
        list.add(p2);

        JsonValue json = mapper.toJsonValue(list);
        assertEquals(jakarta.json.JsonValue.ValueType.ARRAY, json.getValueType());
    }
}
