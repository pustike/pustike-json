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

import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for edge cases: invalid JSON, missing constructors, empty POJOs, and cache clearing.
 */
public class EdgeCasesTest {

    private final ObjectMapper mapper = new ObjectMapper(new TypeConverter());

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void readValue_invalidJson() {
        assertThrows(JsonException.class, () -> mapper.readValue("{invalid}", TestPojo.SimplePojo.class));
    }

    @Test
    public void readValue_noArgConstructorRequired() {
        // Classes without no-arg constructor should fail gracefully
        assertThrows(JsonException.class, () -> mapper.readValue("{\"name\":\"a\",\"age\":1}", TestPojo.PersonRecord.class));
    }

    @Test
    public void toJsonValue_emptyPojo() {
        TestPojo.EmptyPojo pojo = new TestPojo.EmptyPojo();
        JsonValue json = mapper.toJsonValue(pojo);
        JsonObject obj = (JsonObject) json;
        assertEquals(0, obj.size());
    }

    @Test
    public void clearCache() {
        // Simply verify it doesn't throw
        ObjectMapper.clearCache();
    }
}
