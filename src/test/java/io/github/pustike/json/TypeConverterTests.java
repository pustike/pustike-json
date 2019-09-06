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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TypeConverterTests {
    private static TypeConverter typeConverter;

    @BeforeAll
    public static void setUp() {
        typeConverter = new TypeConverter();
    }

    @Test
    public void canConvert() {
        assertTrue(typeConverter.canConvert(String.class, Integer.class));
        assertFalse(typeConverter.canConvert(String.class, String[].class));
    }

    @Test
    public void canConvertAssignable() {
        assertTrue(typeConverter.canConvert(String.class, String.class));
        assertTrue(typeConverter.canConvert(Integer.class, Number.class));
        assertTrue(typeConverter.canConvert(boolean.class, boolean.class));
        assertTrue(typeConverter.canConvert(boolean.class, Boolean.class));
    }

    @Test
    public void canConvertFromClassSourceTypeToNullTargetType() {
        assertThrows(NullPointerException.class, () -> typeConverter.convert(String.class, null));
    }

    @Test
    public void canConvertNullSourceType() {
        assertTrue(typeConverter.canConvert(null, Integer.class));
    }

    @Test
    public void convertNullSource() {
        assertNull(typeConverter.convert(null, Integer.class));
        assertNull(typeConverter.convert(null, Integer.class));
    }

    @Test
    public void convertNullSourcePrimitiveTarget() {
        assertNull(typeConverter.convert(null, int.class));
    }

    @Test
    public void convertAssignableSource() {
        assertEquals(Boolean.FALSE, typeConverter.convert(false, Boolean.class));
        assertEquals(false, typeConverter.convert(Boolean.FALSE, boolean.class));
    }

    @Test
    public void convertVoidTypes() {
        assertFalse(typeConverter.canConvert(void.class, String.class));
        assertFalse(typeConverter.canConvert(String.class, void.class));
    }

    @AfterAll
    public static void tearDown() {
        typeConverter.invalidate();
        typeConverter = null;
    }
}
