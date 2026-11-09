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

import jakarta.json.JsonValue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for thread safety of {@link ObjectMapper} under concurrent access.
 */
public class ThreadSafetyTest {

    @AfterEach
    public void tearDown() {
        ObjectMapper.clearCache();
    }

    @Test
    public void concurrentAccess() throws InterruptedException {
        ObjectMapper sharedMapper = new ObjectMapper(new TypeConverter());
        Thread[] threads = new Thread[10];
        Exception[] errors = new Exception[10];

        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        TestPojo.SimplePojo pojo = new TestPojo.SimplePojo();
                        pojo.setName("thread-" + index + "-" + j);
                        pojo.setAge(index * 100 + j);
                        JsonValue json = sharedMapper.toJsonValue(pojo);
                        assertNotNull(json);
                    }
                } catch (Exception e) {
                    synchronized (errors) {
                        errors[index] = e;
                    }
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        for (Exception e : errors) {
            if (e != null) {
                fail("Concurrent access failed: " + e.getMessage());
            }
        }
    }
}
