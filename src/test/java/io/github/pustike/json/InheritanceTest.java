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
 * Tests for class inheritance handling during serialization and deserialization via {@link ObjectMapper}.
 */
public class InheritanceTest {

    private final ObjectMapper mapper = new ObjectMapper(new TypeConverter());

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void toJsonValue_inheritance() {
        TestPojo.ChildPojo child = new TestPojo.ChildPojo();
        child.setParentField("parent");
        child.setChildField("child");

        JsonValue json = mapper.toJsonValue(child);
        JsonObject obj = (JsonObject) json;
        assertEquals("parent", obj.getString("parentField"));
        assertEquals("child", obj.getString("childField"));
    }

    @Test
    public void readValue_inheritance() {
        String json = "{\"parentField\":\"p\",\"childField\":\"c\"}";
        TestPojo.ChildPojo child = mapper.readValue(json, TestPojo.ChildPojo.class);
        assertEquals("p", child.getParentField());
        assertEquals("c", child.getChildField());
    }
}
