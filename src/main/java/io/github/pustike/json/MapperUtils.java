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

import java.lang.invoke.MethodHandle;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.json.JsonException;

class MapperUtils {
    private static final Map<Class<?>, ClassMetadata> classMetadataCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Map<String, String>>> includedFieldsCache = new ConcurrentHashMap<>();

    static ClassMetadata getMetadata(Class<?> clazz) {
        return classMetadataCache.computeIfAbsent(clazz, ClassMetadata::new);
    }

    @SuppressWarnings("unchecked")
    static <V> V invokeConstructor(Class<?> clazz) {
        try {
            MethodHandle methodHandle = getMetadata(clazz).getConstructor();
            return (V) methodHandle.invoke();
        } catch (Throwable e) {
            throw new JsonException("unable to invoke constructor of class: " + clazz, e);
        }
    }

    static Map<Object, Object> createObjectMapInstance(Class<?> valueType) {
        if (valueType != Object.class && !valueType.isInterface()) {
            return invokeConstructor(valueType);
        } else {
            return new HashMap<>();
        }
    }

    static <V> Collection<V> createCollectionInstance(Class<?> collectionType) {
        if (Object.class.equals(collectionType) || List.class.equals(collectionType)) {
            return new ArrayList<>();
        } else if (Set.class.equals(collectionType)) {
            return new HashSet<>();
        } else if (!collectionType.isInterface()) {
            return invokeConstructor(collectionType);
        } else {
            throw new JsonException("unable to create collection instance: " + collectionType);
        }
    }

    static Map.Entry<Class<?>, Class<?>> getTypeArguments(Type type) {
        if (type instanceof Class) {
            return new AbstractMap.SimpleImmutableEntry<>((Class<?>) type, (Class<?>) type);
        } else if (type instanceof ParameterizedType parameterizedType) {
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length > 0) {
                // return getTypeArgument(typeArgument); // for multi-level inner type!
                Type typeArgument = typeArguments[0];
                if (typeArgument instanceof Class) {
                    return new AbstractMap.SimpleImmutableEntry<>(rawType, (Class<?>) typeArgument);
                } else if (typeArgument instanceof ParameterizedType) {
                    return new AbstractMap.SimpleImmutableEntry<>(rawType, (Class<?>)
                            ((ParameterizedType) typeArgument).getRawType());
                }
            }
            return new AbstractMap.SimpleImmutableEntry<>(rawType, rawType);
        } else {
            throw new IllegalArgumentException("Type parameter " + type.toString() + " not a class or " +
                    "parameterized type whose raw type is a class");
        }
    }

    static Map<String, String> findFieldInclusionMap(Class<?> objectClass, String context) {
        if (context == null) {
            return Map.of();
        }
        JsonInclude[] jsonIncludes = getMetadata(objectClass).getAnnotations();
        if (jsonIncludes.length > 0) {// this way PageData without the @JsonInclude is skipped
            JsonInclude selectedInclude = getSelectedInclude(context, jsonIncludes);
            if (selectedInclude != null) {
                // Build the field context map inside computeIfAbsent to avoid race conditions
                // where the computed value could be discarded by another thread
                return includedFieldsCache.computeIfAbsent(objectClass, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(context, k -> buildFieldCtxMap(selectedInclude.fields()));
            }
        }
        return Map.of();
    }

    private static Map<String, String> buildFieldCtxMap(String[] fields) {
        Map<String, String> fieldCtxMap = new HashMap<>(fields.length);
        for (String fieldName : fields) {
            int fgIndex = fieldName.indexOf('@');
            String joinFieldGroup = null;
            if(fgIndex != -1) {
                joinFieldGroup = fieldName.substring(fgIndex + 1);
                fieldName = fieldName.substring(0, fgIndex);
            }
            fieldCtxMap.put(fieldName, joinFieldGroup);
        }
        return fieldCtxMap;
    }

    private static JsonInclude getSelectedInclude(String context, JsonInclude[] jsonIncludes) {
        JsonInclude defaultInclude = null, requestedInclude = null;
        for (JsonInclude jsonInclude : jsonIncludes) {
            if (defaultInclude == null && Objects.equals(jsonInclude.type(), "")) {
                defaultInclude = jsonInclude;
            }
            if (Objects.equals(jsonInclude.type(), context)) {
                requestedInclude = jsonInclude;
                break;
            }
        }
        return requestedInclude == null ? defaultInclude : requestedInclude;
    }

    static void clearCache() {
        classMetadataCache.clear();
        includedFieldsCache.clear();
    }
}

