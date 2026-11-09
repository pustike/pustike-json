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
 * Tests for primitive and wrapper type serialization/deserialization via {@link ObjectMapper}.
 */
public class PrimitiveTypesTest {

    private final ObjectMapper mapper = new ObjectMapper(new TypeConverter());

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void toJsonValue_allPrimitiveTypes() {
        TestPojo.PrimitivePojo pojo = new TestPojo.PrimitivePojo();
        pojo.setBoolVal(true);
        pojo.setByteVal((byte) 10);
        pojo.setShortVal((short) 20);
        pojo.setIntVal(30);
        pojo.setLongVal(40L);
        pojo.setFloatVal(3.14f);
        pojo.setDoubleVal(2.718);
        pojo.setCharVal('X');

        JsonValue json = mapper.toJsonValue(pojo);
        JsonObject obj = (JsonObject) json;
        assertTrue(obj.getBoolean("boolVal"));
        assertEquals(10, obj.getInt("byteVal"));
        assertEquals(20, obj.getInt("shortVal"));
        assertEquals(30, obj.getInt("intVal"));
        assertEquals(40, obj.getJsonNumber("longVal").longValue());
        assertEquals(3.14, obj.getJsonNumber("floatVal").doubleValue(), 0.001);
        assertEquals(2.718, obj.getJsonNumber("doubleVal").doubleValue(), 0.001);
        assertEquals("X", obj.getString("charVal"));
    }

    @Test
    public void readValue_allPrimitiveTypes() {
        String json = "{\"boolVal\":true,\"byteVal\":10,\"shortVal\":20,\"intVal\":30,\"longVal\":40,\"floatVal\":3.14,\"doubleVal\":2.718,\"charVal\":\"X\"}";
        TestPojo.PrimitivePojo pojo = mapper.readValue(json, TestPojo.PrimitivePojo.class);
        assertTrue(pojo.isBoolVal());
        assertEquals((byte) 10, pojo.getByteVal());
        assertEquals((short) 20, pojo.getShortVal());
        assertEquals(30, pojo.getIntVal());
        assertEquals(40L, pojo.getLongVal());
        assertEquals(3.14f, pojo.getFloatVal(), 0.001f);
        assertEquals(2.718, pojo.getDoubleVal(), 0.001);
        assertEquals('X', pojo.getCharVal());
    }

    @Test
    public void toJsonValue_primitiveDefaults() {
        // Default primitive values should still be serialized (not null)
        TestPojo.PrimitivePojo pojo = new TestPojo.PrimitivePojo();
        JsonValue json = mapper.toJsonValue(pojo);
        JsonObject obj = (JsonObject) json;
        assertFalse(obj.getBoolean("boolVal"));
        assertEquals(0, obj.getInt("byteVal"));
        assertEquals(0, obj.getInt("intVal"));
        assertEquals(0, obj.getJsonNumber("longVal").longValue());
    }

    @Test
    public void readValue_wrapperTypes() {
        String json = "{\"name\":\"test\",\"count\":42,\"active\":true}";
        TestPojo.WrapperPojo pojo = mapper.readValue(json, TestPojo.WrapperPojo.class);
        assertEquals("test", pojo.getName());
        assertEquals(42, pojo.getCount());
        assertTrue(pojo.getActive());
    }

    @Test
    public void readValue_nullWrapperTypes() {
        String json = "{\"name\":null,\"count\":null}";
        TestPojo.WrapperPojo pojo = mapper.readValue(json, TestPojo.WrapperPojo.class);
        assertNull(pojo.getName());
        assertNull(pojo.getCount());
    }
}
