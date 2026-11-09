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
 * Tests for read-only and write-only property handling via {@link ObjectMapper}.
 */
public class SpecialPropertiesTest {

    private final ObjectMapper mapper = new ObjectMapper(new TypeConverter());

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void toJsonValue_readOnlyProperty() {
        TestPojo.ReadOnlyPojo pojo = new TestPojo.ReadOnlyPojo("computed");
        JsonValue json = mapper.toJsonValue(pojo);
        JsonObject obj = (JsonObject) json;
        assertEquals("computed", obj.getString("computedValue"));
    }

    @Test
    public void readValue_writeOnlyProperty() {
        String json = "{\"inputValue\":\"received\"}";
        TestPojo.WriteOnlyPojo pojo = mapper.readValue(json, TestPojo.WriteOnlyPojo.class);
        // Write-only: can be set from JSON but has no getter
        assertNotNull(pojo);
    }
}
