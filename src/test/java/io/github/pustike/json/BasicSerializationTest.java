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

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for basic POJO serialization to JSON via {@link ObjectMapper}.
 */
public class BasicSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper(new TypeConverter());

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void toJsonValue_simplePojo() {
        TestPojo.SimplePojo pojo = new TestPojo.SimplePojo();
        pojo.setName("test");
        pojo.setAge(42);
        pojo.setActive(true);

        JsonValue json = mapper.toJsonValue(pojo);
        assertInstanceOf(JsonObject.class, json);
        JsonObject obj = (JsonObject) json;
        assertEquals("test", obj.getString("name"));
        assertEquals(42, obj.getInt("age"));
        assertTrue(obj.getBoolean("active"));
    }

    @Test
    public void toJsonValue_nullValue() {
        JsonValue json = mapper.toJsonValue((Object) null);
        assertEquals(JsonValue.NULL, json);
    }

    @Test
    public void toJsonValue_booleanValue() {
        assertEquals(JsonValue.TRUE, mapper.toJsonValue(true));
        assertEquals(JsonValue.FALSE, mapper.toJsonValue(false));
    }

    @Test
    public void toJsonValue_numberValue() {
        JsonValue json = mapper.toJsonValue(42);
        assertEquals(JsonValue.ValueType.NUMBER, json.getValueType());
    }

    @Test
    public void toJsonValue_stringValue() {
        TypeConverter tc = new TypeConverter();
        ObjectMapper m = new ObjectMapper(tc);
        JsonValue json = m.toJsonValue("hello");
        assertEquals(JsonValue.ValueType.STRING, json.getValueType());
    }

    @Test
    public void toJsonValue_nullFieldsOmitted() {
        TestPojo.SimplePojo pojo = new TestPojo.SimplePojo();
        pojo.setName(null);
        pojo.setAge(42);

        JsonValue json = mapper.toJsonValue(pojo);
        JsonObject obj = (JsonObject) json;
        assertNull(obj.get("name"));
        assertEquals(42, obj.getInt("age"));
    }

    @Test
    public void toJsonValue_characterValue() {
        JsonValue json = mapper.toJsonValue('X');
        assertEquals(JsonValue.ValueType.STRING, json.getValueType());
        assertEquals("X", ((jakarta.json.JsonString) json).getString());
    }

    @Test
    public void toJsonValue_characterWrapperValue() {
        Character ch = 'A';
        JsonValue json = mapper.toJsonValue(ch);
        assertEquals(JsonValue.ValueType.STRING, json.getValueType());
        assertEquals("A", ((jakarta.json.JsonString) json).getString());
    }
}
