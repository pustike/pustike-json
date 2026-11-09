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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TypeConverter} integration with {@link ObjectMapper} for date, time, enum, and instant types.
 */
public class TypeConversionTest {

    private final ObjectMapper mapper = new ObjectMapper(new TypeConverter());

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void readValue_dateConversion() {
        String json = "{\"date\":\"2024-01-15\"}";
        TestPojo.DatePojo pojo = mapper.readValue(json, TestPojo.DatePojo.class);
        assertEquals(LocalDate.of(2024, 1, 15), pojo.getDate());
    }

    @Test
    public void toJsonValue_dateConversion() {
        TestPojo.DatePojo pojo = new TestPojo.DatePojo();
        pojo.setDate(LocalDate.of(2024, 1, 15));

        JsonValue json = mapper.toJsonValue(pojo);
        JsonObject obj = (JsonObject) json;
        assertEquals("2024-01-15", obj.getString("date"));
    }

    @Test
    public void readValue_enumConversion() {
        String json = "{\"status\":\"ACTIVE\"}";
        TestPojo.EnumPojo pojo = mapper.readValue(json, TestPojo.EnumPojo.class);
        assertEquals(TestPojo.Status.ACTIVE, pojo.getStatus());
    }

    @Test
    public void toJsonValue_enumConversion() {
        TestPojo.EnumPojo pojo = new TestPojo.EnumPojo();
        pojo.setStatus(TestPojo.Status.INACTIVE);

        JsonValue json = mapper.toJsonValue(pojo);
        JsonObject obj = (JsonObject) json;
        assertEquals("INACTIVE", obj.getString("status"));
    }

    @Test
    public void readValue_localTimeConversion() {
        String json = "{\"time\":\"14:30:00\"}";
        TestPojo.TimePojo pojo = mapper.readValue(json, TestPojo.TimePojo.class);
        assertEquals(LocalTime.of(14, 30, 0), pojo.getTime());
    }

    @Test
    public void toJsonValue_localTimeConversion() {
        TestPojo.TimePojo pojo = new TestPojo.TimePojo();
        pojo.setTime(LocalTime.of(14, 30, 0));

        JsonValue json = mapper.toJsonValue(pojo);
        JsonObject obj = (JsonObject) json;
        // LocalTime.toString() drops trailing zeros: 14:30:00 -> "14:30"
        assertEquals("14:30", obj.getString("time"));
    }

    @Test
    public void readValue_instantConversion() {
        String json = "{\"instant\":\"2024-01-15T12:00:00Z\"}";
        TestPojo.InstantPojo pojo = mapper.readValue(json, TestPojo.InstantPojo.class);
        assertEquals(Instant.parse("2024-01-15T12:00:00Z"), pojo.getInstant());
    }

    @Test
    public void toJsonValue_instantConversion() {
        TestPojo.InstantPojo pojo = new TestPojo.InstantPojo();
        pojo.setInstant(Instant.parse("2024-01-15T12:00:00Z"));

        JsonValue json = mapper.toJsonValue(pojo);
        JsonObject obj = (JsonObject) json;
        assertEquals("2024-01-15T12:00:00Z", obj.getString("instant"));
    }
}
