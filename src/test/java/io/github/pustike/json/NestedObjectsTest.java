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
 * Tests for nested object serialization via {@link ObjectMapper}.
 */
public class NestedObjectsTest {

    private final ObjectMapper mapper = new ObjectMapper(new TypeConverter());

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void toJsonValue_nestedObject() {
        TestPojo.NestedPojo outer = new TestPojo.NestedPojo();
        outer.setName("outer");
        TestPojo.NestedPojo.Inner inner = new TestPojo.NestedPojo.Inner();
        inner.setInnerName("inner");
        outer.setInner(inner);

        JsonValue json = mapper.toJsonValue(outer);
        JsonObject obj = (JsonObject) json;
        assertEquals("outer", obj.getString("name"));
        JsonObject innerObj = obj.getJsonObject("inner");
        assertNotNull(innerObj);
        assertEquals("inner", innerObj.getString("innerName"));
    }
}
