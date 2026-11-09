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
 * Tests for {@link JsonIgnore} annotation behavior during serialization and deserialization.
 */
public class IgnoredFieldsTest {

    private final ObjectMapper mapper = new ObjectMapper(new TypeConverter());

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void toJsonValue_ignoredField() {
        TestPojo.IgnoredPojo pojo = new TestPojo.IgnoredPojo();
        pojo.setVisible("visible");
        pojo.setHidden("hidden");

        JsonValue json = mapper.toJsonValue(pojo);
        JsonObject obj = (JsonObject) json;
        assertEquals("visible", obj.getString("visible"));
        assertNull(obj.get("hidden"));
    }

    @Test
    public void toJsonValue_classLevelIgnored() {
        TestPojo.ClassLevelIgnoredPojo pojo = new TestPojo.ClassLevelIgnoredPojo();
        pojo.setField1("value1");
        pojo.setField2("value2");

        JsonValue json = mapper.toJsonValue(pojo);
        JsonObject obj = (JsonObject) json;
        assertEquals(0, obj.size()); // All fields ignored due to class-level @JsonIgnore
    }

    @Test
    public void readValue_classLevelIgnored() {
        String json = "{\"field1\":\"v1\",\"field2\":\"v2\"}";
        TestPojo.ClassLevelIgnoredPojo pojo = mapper.readValue(json, TestPojo.ClassLevelIgnoredPojo.class);
        // Fields should remain null since all are ignored at class level
        assertNull(pojo.getField1());
        assertNull(pojo.getField2());
    }

    @Test
    public void readValue_ignoredField() {
        String json = "{\"visible\":\"v\",\"hidden\":\"h\"}";
        TestPojo.IgnoredPojo pojo = mapper.readValue(json, TestPojo.IgnoredPojo.class);
        assertEquals("v", pojo.getVisible());
        assertNull(pojo.getHidden());
    }
}
