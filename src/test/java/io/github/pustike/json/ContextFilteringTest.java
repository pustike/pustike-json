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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for context-aware field filtering via {@link JsonInclude} annotations.
 */
public class ContextFilteringTest {

    private final ObjectMapper mapper = new ObjectMapper(new TypeConverter());

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void toJsonValue_withContext() {
        TestPojo.ContextPojo pojo = new TestPojo.ContextPojo();
        pojo.setId(1L);
        pojo.setName("test");
        pojo.setDescription("long description");
        pojo.setCreatedAt(LocalDateTime.now());

        // "select" context should only include id and name
        JsonValue json = mapper.toJsonValue(pojo, "select");
        JsonObject obj = (JsonObject) json;
        assertEquals(2, obj.size());
        assertEquals(1, obj.getInt("id"));
        assertEquals("test", obj.getString("name"));
        assertNull(obj.get("description"));
        assertNull(obj.get("createdAt"));
    }

    @Test
    public void toJsonValue_withDefaultContext() {
        TestPojo.ContextPojo pojo = new TestPojo.ContextPojo();
        pojo.setId(1L);
        pojo.setName("test");
        pojo.setDescription("long description");
        pojo.setCreatedAt(LocalDateTime.now());

        // "" (default) context should include id, name, description
        JsonValue json = mapper.toJsonValue(pojo, "");
        JsonObject obj = (JsonObject) json;
        assertEquals(3, obj.size());
        assertEquals(1, obj.getInt("id"));
        assertEquals("test", obj.getString("name"));
        assertEquals("long description", obj.getString("description"));
    }

    // -- Nested context propagation (fieldName@context syntax) --

    @Test
    public void toJsonValue_nestedContextPropagation() {
        TestPojo.OuterPojo outer = new TestPojo.OuterPojo();
        outer.setName("outer");
        TestPojo.InnerContextPojo inner = new TestPojo.InnerContextPojo();
        inner.setTitle("inner title");
        inner.setDetails("inner details");
        outer.setInner(inner);

        // "display" context on OuterPojo includes "name" and "inner@summary"
        // "summary" context on InnerContextPojo should only include "title"
        JsonValue json = mapper.toJsonValue(outer, "display");
        JsonObject obj = (JsonObject) json;
        assertEquals("outer", obj.getString("name"));
        JsonObject innerObj = obj.getJsonObject("inner");
        assertNotNull(innerObj);
        assertEquals("inner title", innerObj.getString("title"));
        assertNull(innerObj.get("details")); // details excluded by "summary" context
    }

    @Test
    public void toJsonValue_nestedContextPropagation_noNestedContext() {
        TestPojo.OuterPojo outer = new TestPojo.OuterPojo();
        outer.setName("outer");
        TestPojo.InnerContextPojo inner = new TestPojo.InnerContextPojo();
        inner.setTitle("inner title");
        inner.setDetails("inner details");
        outer.setInner(inner);

        // "full" context on OuterPojo includes "name" and "inner" (no @context)
        // inner should serialize all non-null fields
        JsonValue json = mapper.toJsonValue(outer, "full");
        JsonObject obj = (JsonObject) json;
        assertEquals("outer", obj.getString("name"));
        JsonObject innerObj = obj.getJsonObject("inner");
        assertNotNull(innerObj);
        assertEquals("inner title", innerObj.getString("title"));
        assertEquals("inner details", innerObj.getString("details"));
    }

    @Test
    public void toJsonValue_nestedContextPropagation_emptyNestedContext() {
        TestPojo.OuterPojo outer = new TestPojo.OuterPojo();
        outer.setName("outer");
        TestPojo.InnerContextPojo inner = new TestPojo.InnerContextPojo();
        inner.setTitle("inner title");
        inner.setDetails("inner details");
        outer.setInner(inner);

        // "compact" context on OuterPojo includes "name" and "inner@" (empty nested context = default)
        // InnerContextPojo default ("") context includes "title" and "details"
        JsonValue json = mapper.toJsonValue(outer, "compact");
        JsonObject obj = (JsonObject) json;
        assertEquals("outer", obj.getString("name"));
        JsonObject innerObj = obj.getJsonObject("inner");
        assertNotNull(innerObj);
        assertEquals("inner title", innerObj.getString("title"));
        assertEquals("inner details", innerObj.getString("details"));
    }
}
