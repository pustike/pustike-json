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
import jakarta.json.JsonValue;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for nested object depth limits in {@link ObjectMapper} to prevent stack overflow.
 */
public class NestingDepthTest {

    private final ObjectMapper mapper = new ObjectMapper(new TypeConverter());

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void toJsonValue_nestedLevelLimit_exceeded() {
        // Build a self-referencing structure that exceeds the max depth of 5
        TestPojo.SelfRefPojo pojo = new TestPojo.SelfRefPojo();
        pojo.setName("root");
        pojo.setChild(pojo); // circular reference creates infinite nesting

        JsonException ex = assertThrows(JsonException.class, () -> mapper.toJsonValue(pojo));
        assertTrue(ex.getMessage().contains("level exceeds maximum"));
    }

    @Test
    public void toJsonValue_nestedLevelLimit_atBoundary() {
        // Every field access in createJsonObject increments the level by 1, including
        // primitive/String fields. So a chain of objects where each has 2 fields (name + child)
        // means: root(0) -> name(1), child(1) -> name(2), child(2) -> ...
        // With 3 objects: root(0) -> l1(1) -> l2(2), l2's fields at level 3, l2.child=null skipped.
        // root(0) -> name(1, String), child(1) -> l1(1) -> name(2, String), child(2) -> l2(2) -> name(3, String), child=null skipped
        // Max level reached: 3 (for l2's name field). Safe.
        TestPojo.LevelPojo l2 = new TestPojo.LevelPojo("level2", null);
        TestPojo.LevelPojo l1 = new TestPojo.LevelPojo("level1", l2);
        TestPojo.LevelPojo root = new TestPojo.LevelPojo("root", l1);

        JsonValue json = mapper.toJsonValue(root);
        assertNotNull(json);
    }

    @Test
    public void toJsonValue_nestedLevelLimit_justOverBoundary() {
        // MAX_NESTING_DEPTH is 15. Build a chain of 17 objects so that
        // the 17th object's fields are processed at level 16, which exceeds 15.
        // root(0) -> l1(1) -> l2(2) -> ... -> l16(16) -> throws
        TestPojo.LevelPojo end = new TestPojo.LevelPojo("end", null);
        TestPojo.LevelPojo current = end;
        for (int i = 15; i >= 1; i--) {
            current = new TestPojo.LevelPojo("level" + i, current);
        }
        TestPojo.LevelPojo root = new TestPojo.LevelPojo("root", current);

        JsonException ex = assertThrows(JsonException.class, () -> mapper.toJsonValue(root));
        assertTrue(ex.getMessage().contains("level exceeds maximum"));
    }

    @Test
    public void toJsonValue_nestedLevelLimit_nullDoesNotCount() {
        // Verify that null field values are skipped before level increment in createJsonObject.
        // 4 objects with null child at the end: root(0)->l1(1)->l2(2)->l3(3, child=null)
        // l3's child=null is skipped by null check in createJsonObject, no level 4 call for it.
        TestPojo.LevelPojo l3 = new TestPojo.LevelPojo("level3", null);
        TestPojo.LevelPojo l2 = new TestPojo.LevelPojo("level2", l3);
        TestPojo.LevelPojo l1 = new TestPojo.LevelPojo("level1", l2);
        TestPojo.LevelPojo root = new TestPojo.LevelPojo("root", l1);

        JsonValue json = mapper.toJsonValue(root);
        assertNotNull(json);
    }

    @Test
    public void toJsonValue_nestedLevelLimit_deepList() {
        // MAX_NESTING_DEPTH is 15. Build deeply nested lists exceeding 16 levels.
        // root (level 0) -> l1 (1) -> l2 (2) -> ... -> l16 (16) -> "leaf" (17) -> throws
        List<Object> deep = List.of("leaf");
        for (int i = 0; i < 16; i++) {
            deep = List.of(deep);
        }
        List<Object> root = List.of(deep);
        JsonException ex = assertThrows(JsonException.class, () -> mapper.toJsonValue(root));
        assertTrue(ex.getMessage().contains("level exceeds maximum"));
    }

    @Test
    public void toJsonValue_nestedLevelLimit_deepMap() {
        // MAX_NESTING_DEPTH is 15. Build deeply nested maps exceeding 16 levels.
        // root (level 0) -> m1 (1) -> m2 (2) -> ... -> m16 (16) -> "value" (17) -> throws
        Map<String, Object> deep = Map.of("key", "value");
        for (int i = 0; i < 16; i++) {
            deep = Map.of("nested", deep);
        }
        Map<String, Object> root = Map.of("nested", deep);
        JsonException ex = assertThrows(JsonException.class, () -> mapper.toJsonValue(root));
        assertTrue(ex.getMessage().contains("level exceeds maximum"));
    }
}
