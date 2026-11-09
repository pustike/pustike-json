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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for basic JSON deserialization to POJOs via {@link ObjectMapper}.
 */
public class BasicDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper(new TypeConverter());

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void readValue_simplePojo() {
        String json = "{\"name\":\"test\",\"age\":42,\"active\":true}";
        TestPojo.SimplePojo pojo = mapper.readValue(json, TestPojo.SimplePojo.class);
        assertNotNull(pojo);
        assertEquals("test", pojo.getName());
        assertEquals(42, pojo.getAge());
        assertTrue(pojo.isActive());
    }

    @Test
    public void readValue_toMap() {
        String json = "{\"name\":\"test\",\"age\":42}";
        Map<?, ?> map = mapper.readValue(json, Map.class);
        assertEquals("test", map.get("name"));
        assertEquals(42, map.get("age"));
    }

    @Test
    public void readValue_toObject() {
        String json = "{\"name\":\"test\",\"age\":42}";
        Map<?, ?> map = mapper.readValue(json, Object.class);
        assertEquals("test", map.get("name"));
        assertEquals(42, map.get("age"));
    }

    @Test
    public void readValue_unknownFieldsIgnored() {
        String json = "{\"name\":\"test\",\"age\":42,\"unknownField\":\"ignored\"}";
        TestPojo.SimplePojo pojo = mapper.readValue(json, TestPojo.SimplePojo.class);
        assertEquals("test", pojo.getName());
        assertEquals(42, pojo.getAge());
    }

    @Test
    public void readValue_emptyObject() {
        String json = "{}";
        Map<?, ?> map = mapper.readValue(json, Map.class);
        assertEquals(0, map.size());
    }

    @Test
    public void readValue_emptyArray() {
        String json = "[]";
        List<?> list = mapper.readValue(json, List.class);
        assertEquals(0, list.size());
    }

    @Test
    public void readValue_byteArray() {
        String json = "{\"name\":\"test\"}";
        TestPojo.SimplePojo pojo = mapper.readValue(json.getBytes(), TestPojo.SimplePojo.class);
        assertEquals("test", pojo.getName());
    }
}
