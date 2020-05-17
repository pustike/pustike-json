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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import jakarta.json.JsonException;

class MapperContext {
    private static final Map<Class<?>, List<Field>> classFieldListCache = new WeakHashMap<>();
    private final String context;
    private Integer contextLevel;

    MapperContext(String context) {
        this.context = context;
    }

    static List<Field> findAllFields(Class<?> type) {
        return classFieldListCache.computeIfAbsent(type, clazz -> {
            List<Field> fieldList = new ArrayList<>();
            for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.getName().startsWith("this$")
                            || field.getName().startsWith("$")) {
                        continue;
                    }
                    fieldList.add(field);
                }
            }
            return fieldList;
        });
    }

    static Object getFieldValue(Object instance, Field field) {
        try {
            String fieldName = field.getName();
            char c = Character.toUpperCase(fieldName.charAt(0));
            Method method = instance.getClass().getMethod("get" + c + fieldName.substring(1));
            if (!method.trySetAccessible()) {
                throw new JsonException("couldn't enable access to method: " + method);
            }
            return method.invoke(instance);
        } catch (Exception e) {
            try {
                if (!field.trySetAccessible()) {
                    throw new JsonException("couldn't enable access to field: " + field);
                }
                return field.get(instance);
            } catch (IllegalAccessException e2) {
                throw new JsonException("error when getting field value!", e2);
            }
        }
    }

    static <T> void setFieldValue(T instance, Field field, Object fieldValue) throws Exception {
        try {
            String fieldName = field.getName();
            char c = Character.toUpperCase(fieldName.charAt(0));
            Method method = instance.getClass().getMethod("set" + c + fieldName.substring(1));
            if (!method.trySetAccessible()) {
                throw new JsonException("couldn't enable access to method: " + method);
            }
            method.invoke(instance, fieldValue);
        } catch (Exception e) {
            if (!field.trySetAccessible()) {
                throw new JsonException("couldn't enable access to field: " + field);
            }
            field.set(instance, fieldValue);
        }
    }

    List<String> findIncludedFields(Class<?> objectClass, int level) {
        if (context == null) {
            return List.of();
        }
        JsonInclude[] jsonIncludes = objectClass.getAnnotationsByType(JsonInclude.class);
        if (jsonIncludes.length > 0) {// this way PageData without the @JsonInclude is skipped
            JsonInclude defaultInclude = null, requestedInclude = null;
            for (JsonInclude jsonInclude : jsonIncludes) {
                if (defaultInclude == null && Objects.equals(jsonInclude.type(), "")) {
                    defaultInclude = jsonInclude;
                }
                String jsonContext = contextLevel == null || contextLevel == level ? context : "";
                if (Objects.equals(jsonInclude.type(), jsonContext)) {
                    requestedInclude = jsonInclude;
                    if (contextLevel == null) {
                        contextLevel = level;
                    }
                    break;
                }
            }
            JsonInclude selectedInclude = requestedInclude == null ? defaultInclude : requestedInclude;
            if (selectedInclude != null) {
                return List.of(selectedInclude.fields());
            }
        }
        return List.of();
    }
}
