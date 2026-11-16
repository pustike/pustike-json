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
 * Tests for {@link JsonProperty} annotation behavior during serialization and deserialization.
 */
public class PropertyNamingTest {

    private final ObjectMapper mapper = new ObjectMapper(new TypeConverter());

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void toJsonValue_jsonPropertyName() {
        TestPojo.MappedPojo pojo = new TestPojo.MappedPojo();
        pojo.setFirstName("John");
        pojo.setLastName("Doe");

        JsonValue json = mapper.toJsonValue(pojo);
        JsonObject obj = (JsonObject) json;
        assertEquals("John", obj.getString("first_name"));
        assertEquals("Doe", obj.getString("last_name"));
    }

    @Test
    public void readValue_jsonPropertyName() {
        String json = "{\"first_name\":\"John\",\"last_name\":\"Doe\"}";
        TestPojo.MappedPojo pojo = mapper.readValue(json, TestPojo.MappedPojo.class);
        assertEquals("John", pojo.getFirstName());
        assertEquals("Doe", pojo.getLastName());
    }

    @Test
    public void toJsonValue_methodLevelJsonProperty() {
        TestPojo.MethodMappedPojo pojo = new TestPojo.MethodMappedPojo();
        pojo.setFirstName("Jane");
        pojo.setLastName("Smith");

        JsonValue json = mapper.toJsonValue(pojo);
        JsonObject obj = (JsonObject) json;
        assertEquals("Jane", obj.getString("first_name"));
        assertEquals("Smith", obj.getString("last_name"));
    }

    @Test
    public void readValue_methodLevelJsonProperty() {
        String json = "{\"first_name\":\"Jane\",\"last_name\":\"Smith\"}";
        TestPojo.MethodMappedPojo pojo = mapper.readValue(json, TestPojo.MethodMappedPojo.class);
        assertEquals("Jane", pojo.getFirstName());
        assertEquals("Smith", pojo.getLastName());
    }
}
