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
 * Tests for Java Record serialization and deserialization via {@link ObjectMapper}.
 */
public class RecordTest {

    private final ObjectMapper mapper = new ObjectMapper(new TypeConverter());

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void toJsonValue_record() {
        TestPojo.PersonRecord person = new TestPojo.PersonRecord("Alice", 30);
        JsonValue json = mapper.toJsonValue(person);
        JsonObject obj = (JsonObject) json;
        assertEquals("Alice", obj.getString("name"));
        assertEquals(30, obj.getInt("age"));
    }

    @Test
    public void toJsonValue_record_withJsonIgnore() {
        TestPojo.SensitiveRecord rec = new TestPojo.SensitiveRecord("Bob", "secret123");
        JsonValue json = mapper.toJsonValue(rec);
        JsonObject obj = (JsonObject) json;
        assertEquals("Bob", obj.getString("name"));
        assertNull(obj.get("password")); // password is @JsonIgnore
    }
}
