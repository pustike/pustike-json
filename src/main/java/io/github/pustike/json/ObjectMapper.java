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

import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.*;

import jakarta.json.*;

/**
 * JSON object mapper for bidirectional conversion between Java objects and JSON.
 * <p>
 * This class is thread-safe and instances may be safely shared across threads.
 * The internal {@link TypeConverter} reference is immutable after construction.
 * </p>
 * <p>
 * Null field values are silently omitted from JSON output by default.
 * Unknown JSON fields are silently ignored during deserialization.
 * </p>
 */
public final class ObjectMapper {
    private final TypeConverter typeConverter;
    private final MethodHandles.Lookup lookup;

    /**
     * Constructs an object mapper without a type converter.
     */
    public ObjectMapper() {
        this(null, MethodHandles.lookup());
    }

    /**
     * Constructs an object mapper with the given type converter.
     * @param typeConverter the type converter to use
     */
    public ObjectMapper(TypeConverter typeConverter) {
        this(typeConverter, MethodHandles.lookup());
    }

    /**
     * Constructs an object mapper with the given type converter and lookup.
     * @param typeConverter the type converter to use
     * @param lookup the base lookup to use
     */
    public ObjectMapper(TypeConverter typeConverter, MethodHandles.Lookup lookup) {
        this.typeConverter = typeConverter;
        this.lookup = Objects.requireNonNull(lookup);
    }

    /**
     * Read JSON value and map it into an object of the given type.
     * @param jsonString the JSON string
     * @param valueType the value type
     * @param <V> the type of object
     * @return the mapped object from JSON value
     * @throws JsonException if value can not be converted to object of given type
     */
    public <V> V readValue(String jsonString, Type valueType) throws JsonException {
        try (StringReader reader = new StringReader(jsonString)) {
            return readValue(reader, valueType);
        }
    }

    /**
     * Read JSON value from the reader and map it into an object of the given type.
     * @param reader the reader providing the JSON string
     * @param valueType the value type
     * @param <V> the type of object
     * @return the mapped object from JSON value
     * @throws JsonException if value can not be converted to object of given type
     */
    public <V> V readValue(Reader reader, Type valueType) throws JsonException {
        try (JsonReader jsonReader = Json.createReader(reader)) {
            return toJavaObject(jsonReader.readValue(), valueType);
        } catch (JsonException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonException("Unable to read value from json", e);
        }
    }

    /**
     * Read JSON value from byte array and map it into an object of the given type.
     * @param bytes the byte array of JSON string
     * @param valueType the value type
     * @param <V> the type of object
     * @return the mapped object from JSON value
     * @throws JsonException if value can not be converted to object of given type
     */
    public <V> V readValue(byte[] bytes, Type valueType) throws JsonException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
             JsonReader jsonReader = Json.createReader(inputStream)) {
            return toJavaObject(jsonReader.readValue(), valueType);
        } catch (JsonException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonException("Unable to read value from json", e);
        }
    }

    /**
     * Map JSON value into a Java object of the given type.
     * @param jsonValue the JSON value instance
     * @param valueType the value type
     * @param <V> the type of object
     * @return the mapped object from JSON value
     * @throws JsonException if value can not be converted to object of given type
     */
    @SuppressWarnings("unchecked")
    public <V> V toJavaObject(JsonValue jsonValue, Type valueType) {
        Objects.requireNonNull(jsonValue);
        Objects.requireNonNull(valueType);
        if (valueType instanceof Class && JsonValue.class.isAssignableFrom((Class<?>) valueType)) {
            return (V) jsonValue;
        }
        Map.Entry<Class<?>, Class<?>> typeArguments = MapperUtils.getTypeArguments(valueType);
        return createJavaObject(jsonValue, (Class<V>) typeArguments.getValue(), typeArguments.getKey());
    }

    /**
     * Generate the Json value from given object.
     * @param value the value instance
     * @return the Json value
     * @throws JsonException if failed to map the object into a Json value
     */
    public JsonValue toJsonValue(Object value) throws JsonException {
        return toJsonValue(value, null);
    }

    /**
     * Generate the Json value from given object by including fields declared in the context.
     * @param value the value instance
     * @param context the json context to select the relevant {@link JsonIncludes} defined
     * @return the Json value
     * @throws JsonException if failed to map the object into a Json value
     */
    public JsonValue toJsonValue(Object value, String context) {
        try {
            return createJsonValue(value, context, 0);
        } catch (JsonException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonException("error while creating the jsonValue", e);
        }
    }

    /**
     * Clears the internal field accessor cache.
     * This is useful when classes are dynamically modified at runtime (e.g., via bytecode manipulation)
     * and the cached field information needs to be refreshed.
     */
    public static void clearCache() {
        MapperUtils.clearCache();
    }

    /**
     * Maximum allowed nesting depth for JSON serialization.
     * Exceeding this limit throws a {@link jakarta.json.JsonException} to prevent stack overflow
     * from deeply nested or circular references.
     */
    private static final int MAX_NESTING_DEPTH = 15;

    private JsonValue createJsonValue(Object value, String context, int level) {
        if (value == null) {
            return JsonValue.NULL;
        }
        if (level > MAX_NESTING_DEPTH) {
            throw new JsonException("level exceeds maximum of " + MAX_NESTING_DEPTH);
        } else if (value instanceof JsonValue) {
            return (JsonValue) value;
        } else if (value instanceof Boolean) {
            return (Boolean) value ? JsonValue.TRUE : JsonValue.FALSE;
        } else if (value instanceof Number) {
            return Json.createValue((Number) value);
        } else if (value instanceof String) {
            return Json.createValue((String) value);
        } else if (value instanceof Character) {
            return Json.createValue(value.toString());
        } else if (value.getClass().isArray()) {
            return createJsonArray(value, context, level);
        } else if (Iterable.class.isAssignableFrom(value.getClass())) {
            return createJsonArray((Iterable<?>) value, context, level);
        } else if (Map.class.isAssignableFrom(value.getClass())) {
            return createJsonObject((Map<?, ?>) value, context, level);
        }
        if (typeConverter != null) {
            String stringValue = typeConverter.convertSafely(value, String.class);
            if (stringValue != null) {
                return Json.createValue(stringValue);
            }
        }
        return createJsonObject(value, context, level);
    }

    private JsonArray createJsonArray(Object value, String context, int level) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (int i = 0; i < Array.getLength(value); i++) {
            arrayBuilder.add(createJsonValue(Array.get(value, i), context, level + 1));
        }
        return arrayBuilder.build();
    }

    private JsonArray createJsonArray(Iterable<?> iterable, String context, int level) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (Object value : iterable) {
            arrayBuilder.add(createJsonValue(value, context, level + 1));
        }
        return arrayBuilder.build();
    }

    private JsonObject createJsonObject(Map<?, ?> map, String context, int level) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            objectBuilder.add(String.valueOf(entry.getKey()), createJsonValue(entry.getValue(), context, level + 1));
        }
        return objectBuilder.build();
    }

    private JsonObject createJsonObject(Object object, String context, int level) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        try {
            ClassMetadata classMetadata = MapperUtils.getMetadata(object.getClass(), lookup);
            Map<String, PropertyAccessor> propertyAccessorMap = classMetadata.getPropertyAccessorMap();
            Map<String, String> fieldCtxMap = MapperUtils.findFieldInclusionMap(object.getClass(), lookup, context);
            boolean hasContextFilter = !fieldCtxMap.isEmpty();
            for (PropertyAccessor accessor : propertyAccessorMap.values()) {
                if (hasContextFilter) {
                    // Field must be in the inclusion map to be serialized
                    if (!fieldCtxMap.containsKey(accessor.javaName())) {
                        continue;
                    }
                }
                Object fieldValue = accessor.get(object);
                if (fieldValue == null) {
                    continue;
                }
                String nestedCtx = fieldCtxMap.get(accessor.javaName());
                if (nestedCtx == null) {
                    nestedCtx = !hasContextFilter ? context : "";
                }
                objectBuilder.add(accessor.name(), createJsonValue(fieldValue, nestedCtx, level + 1));
            }
        } catch (JsonException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonException("unable to create json object", e);
        }
        return objectBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private <V> V createJavaObject(JsonValue jsonValue, Class<V> valueType, Class<?> collectionType) {
        if (jsonValue instanceof JsonArray) {
            return createArrayInstance((JsonArray) jsonValue, valueType, collectionType);
        } else if (jsonValue instanceof JsonObject) {
            return createObjectInstance((JsonObject) jsonValue, valueType);
        }
        Object value = null;
        if (jsonValue instanceof JsonString) {
            value = ((JsonString) jsonValue).getString();
        } else if (jsonValue instanceof JsonNumber) {
            value = ((JsonNumber) jsonValue).numberValue();
        } else if (jsonValue == JsonValue.TRUE) {
            value = Boolean.TRUE;
        } else if (jsonValue == JsonValue.FALSE) {
            value = Boolean.FALSE;
        }
        if (value == null || valueType.isAssignableFrom(value.getClass())) {
            return (V) value;
        } else if (typeConverter != null) {
            return typeConverter.convertSafely(value, valueType);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <V> V createObjectInstance(JsonObject jsonObject, Class<?> valueType) {
        if (valueType == Object.class || Map.class.isAssignableFrom(valueType)) {// then return a map
            Map<Object, Object> instance = MapperUtils.createObjectMapInstance(valueType, lookup);
            for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
                instance.put(entry.getKey(), toJavaObject(entry.getValue(), Object.class));
            }
            return (V) Collections.unmodifiableMap(instance);
        } else { // try to create the value type instance
            try {
                V instance = MapperUtils.invokeConstructor(valueType, lookup);
                ClassMetadata classMetadata = MapperUtils.getMetadata(valueType, lookup);
                Map<String, PropertyAccessor> propertyAccessorMap = classMetadata.getPropertyAccessorMap();
                for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
                    PropertyAccessor accessor = propertyAccessorMap.get(entry.getKey());
                    if (accessor == null) {
                        continue; // Unknown JSON fields are silently ignored
                    }
                    accessor.set(instance, createJavaObject(entry.getValue(),
                            accessor.typeArgument(), accessor.type()));
                }
                return instance;
            } catch (JsonException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonException("unable to create object instance", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <V> V createArrayInstance(JsonArray jsonArray, Class<V> valueType, Class<?> collectionType) {
        if (collectionType == null) {
            Map.Entry<Class<?>, Class<?>> typeArguments = MapperUtils.getTypeArguments(valueType);
            collectionType = typeArguments.getKey();
            valueType = (Class<V>) typeArguments.getValue();
        }
        // For raw collection types (List, Set, Collection), use Object as element type
        if (valueType.isInterface() && (List.class.isAssignableFrom(valueType)
                || Set.class.isAssignableFrom(valueType)
                || Collection.class.isAssignableFrom(valueType))) {
            valueType = (Class<V>) Object.class;
        }
        if (collectionType.isArray()) {
            V[] instance = (V[]) Array.newInstance(valueType, jsonArray.size());
            for (int i = 0; i < jsonArray.size(); i++) {
                instance[i] = toJavaObject(jsonArray.get(i), valueType);
            }
            return (V) instance;
        } else if (Collection.class.isAssignableFrom(collectionType)) {
            Collection<V> instance = MapperUtils.createCollectionInstance(collectionType, lookup);
            for (JsonValue jsonValue : jsonArray) {
                instance.add(toJavaObject(jsonValue, valueType));
            }
            if (Object.class.equals(collectionType) || List.class.equals(collectionType)) {
                return (V) Collections.unmodifiableList((List<?>) instance);
            } else if (Set.class.equals(collectionType)) {
                return (V) Collections.unmodifiableSet((Set<?>) instance);
            } else {
                return (V) instance;
            }
        }
        throw new IllegalArgumentException("JsonArray can only be converted into an array type!");
    }
}
