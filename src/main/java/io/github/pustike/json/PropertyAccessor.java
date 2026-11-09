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

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import jakarta.json.JsonException;

// --- Public Accessor Model ---
record PropertyAccessor(
        String name,               // Exposed property name (honors @Property)
        String javaName,           // Underlying Java property/field name
        Class<?> type,             // Resolved property type
        Class<?> typeArgument,     // Resolved typeArgument for a generic type (else same as type)
        MethodHandle getter,       // Signature: (Object) -> Object (null if write-only)
        MethodHandle setter,       // Signature: (Object, Object) -> void (null if read-only/final)
        Field backingField,        // Backing field if present (nullable)
        Method getterMethod,       // Getter method if present (nullable)
        Method setterMethod        // Setter method if present (nullable)
) {
    public Object get(Object target) {
        if (getter == null) {
            throw new UnsupportedOperationException("Property '" + name + "' is write-only.");
        }
        try {
            return getter.invokeExact(target);
        } catch (Throwable e) {
            throw new JsonException(String.format("unable to invoke getter for: %s of class: %s", javaName, type), e);
        }
    }

    public void set(Object target, Object value) {
        if (setter == null) {
            throw new UnsupportedOperationException("Property '" + name + "' is read-only.");
        }
        try {
            setter.invokeExact(target, value);
        } catch (Throwable e) {
            throw new JsonException(String.format("unable to invoke setter for: %s of class: %s", javaName, type), e);
        }
    }

    public <A extends Annotation> A findAnnotation(Class<A> annotationClass) {
        A ann = (getterMethod != null) ? getterMethod.getAnnotation(annotationClass) : null;
        if (ann == null && setterMethod != null) {
            ann = setterMethod.getAnnotation(annotationClass);
        }
        if (ann == null && backingField != null) {
            ann = backingField.getAnnotation(annotationClass);
        }
        return ann;
    }
}
