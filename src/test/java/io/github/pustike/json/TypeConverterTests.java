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

import java.time.LocalDate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TypeConverter} standalone behavior.
 */
public class TypeConverterTests {
    private static TypeConverter tc;

    @BeforeAll
    public static void setUp() {
        tc = new TypeConverter();
    }

    @Test
    public void canConvert() {
        assertTrue(tc.canConvert(String.class, Integer.class));
        assertFalse(tc.canConvert(String.class, String[].class));
    }

    @Test
    public void canConvertAssignable() {
        assertTrue(tc.canConvert(String.class, String.class));
        assertTrue(tc.canConvert(Integer.class, Number.class));
        assertTrue(tc.canConvert(boolean.class, boolean.class));
        assertTrue(tc.canConvert(boolean.class, Boolean.class));
    }

    @Test
    public void canConvertFromClassSourceTypeToNullTargetType() {
        assertThrows(NullPointerException.class, () -> tc.convert(String.class, null));
    }

    @Test
    public void canConvertNullSourceType() {
        assertTrue(tc.canConvert(null, Integer.class));
    }

    @Test
    public void convertNullSource() {
        assertNull(tc.convert(null, Integer.class));
        assertNull(tc.convert(null, Integer.class));
    }

    @Test
    public void convertNullSourcePrimitiveTarget() {
        assertNull(tc.convert(null, int.class));
    }

    @Test
    public void convertAssignableSource() {
        assertEquals(Boolean.FALSE, tc.convert(false, Boolean.class));
        assertEquals(false, tc.convert(Boolean.FALSE, boolean.class));
    }

    @Test
    public void convertVoidTypes() {
        assertFalse(tc.canConvert(void.class, String.class));
        assertFalse(tc.canConvert(String.class, void.class));
    }

    @Test
    public void typeConverter_canConvert_sameType() {
        assertTrue(tc.canConvert(String.class, String.class));
    }

    @Test
    public void typeConverter_canConvert_registered() {
        assertTrue(tc.canConvert(String.class, LocalDate.class));
    }

    @Test
    public void typeConverter_canConvert_unregistered() {
        assertFalse(tc.canConvert(String.class, TestPojo.PersonRecord.class));
    }

    @Test
    public void typeConverter_canConvert_nullSource() {
        assertTrue(tc.canConvert(null, String.class));
    }

    @Test
    public void typeConverter_convert_numberToString() {
        String result = tc.convert(42, String.class);
        assertEquals("42", result);
    }

    @Test
    public void typeConverter_convert_stringToNumber() {
        Integer result = tc.convert("42", Integer.class);
        assertEquals(42, result);
    }

    @Test
    public void typeConverter_convertSafely_unregistered() {
        // Should return null instead of throwing
        Object result = tc.convertSafely("test", TestPojo.PersonRecord.class);
        assertNull(result);
    }

    @Test
    public void typeConverter_invalidate() {
        TypeConverter converter = new TypeConverter();
        // Should not throw
        converter.invalidate();
    }

    @Test
    public void typeConverter_convert_boolean() {
        Boolean result = tc.convert("true", Boolean.class);
        assertTrue(result);
    }

    @Test
    public void typeConverter_convert_character() {
        Character result = tc.convert("A", Character.class);
        assertEquals('A', result);
    }

    @AfterAll
    public static void tearDown() {
        tc.invalidate();
        tc = null;
    }
}
