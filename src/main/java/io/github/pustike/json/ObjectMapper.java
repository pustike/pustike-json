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
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import jakarta.json.*;

/**
 * JSON object mapper.
 */
public final class ObjectMapper {
    private final TypeConverter typeConverter;

    /**
     * Constructs an object mapper without a type converter.
     */
    public ObjectMapper() {
        this.typeConverter = null;
    }

    /**
     * Constructs an object mapper with the given type converter.
     * @param typeConverter the type converter to use
     */
    public ObjectMapper(TypeConverter typeConverter) {
        this.typeConverter = typeConverter;
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
        Map.Entry<Class<?>, Class<?>> typeArguments = getTypeArguments(valueType);
        return toJavaObject(jsonValue, (Class<V>) typeArguments.getValue(), typeArguments.getKey());
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
            return toJsonValue(value, new MapperContext(context), 0);
        } catch (JsonException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonException("Unable to write value as string", e);
        }
    }

    private JsonValue toJsonValue(Object value, MapperContext context, int level) {
        if (value == null) {
            return JsonValue.NULL;
        } else if (value instanceof JsonValue) {
            return (JsonValue) value;
        } else if (value instanceof Boolean) {
            return (Boolean) value ? JsonValue.TRUE : JsonValue.FALSE;
        } else if (value instanceof Number) {
            return toJsonNumber((Number) value);
        } else if (value.getClass().isArray()) {
            return toJsonArray(value, context, level);
        } else if (Iterable.class.isAssignableFrom(value.getClass())) {
            return toJsonArray((Iterable<?>) value, context, level);
        } else if (Map.class.isAssignableFrom(value.getClass())) {
            return toJsonObject((Map<?, ?>) value, context, level);
        } else {
            if (typeConverter != null) {
                String stringValue = typeConverter.convertSafely(value, String.class);
                if (stringValue != null) {
                    return Json.createValue(stringValue);
                }
            }
            return toJsonObject(value, context, level);
        }
    }

    private JsonValue toJsonNumber(Number value) {
        if (value instanceof Byte) {
            return Json.createValue((Byte) value);
        } else if (value instanceof Short) {
            return Json.createValue((Short) value);
        } else if (value instanceof Integer) {
            return Json.createValue((Integer) value);
        } else if (value instanceof Long) {
            return Json.createValue((Long) value);
        } else if (value instanceof BigInteger) {
            return Json.createValue((BigInteger) value);
        } else if (value instanceof Float) {
            return Json.createValue((Float) value);
        } else if (value instanceof Double) {
            return Json.createValue((Double) value);
        } else if (value instanceof BigDecimal) {
            return Json.createValue((BigDecimal) value);
        }
        throw new JsonException("Unable to convert value: " + value);
    }

    private JsonArray toJsonArray(Object value, MapperContext context, int level) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (int i = 0; i < Array.getLength(value); i++) {
            arrayBuilder.add(toJsonValue(Array.get(value, i), context, level));
        }
        return arrayBuilder.build();
    }

    private JsonArray toJsonArray(Iterable<?> iterable, MapperContext context, int level) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (Object value : iterable) {
            arrayBuilder.add(toJsonValue(value, context, level));
        }
        return arrayBuilder.build();
    }

    private JsonObject toJsonObject(Map<?, ?> map, MapperContext context, int level) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            objectBuilder.add(String.valueOf(entry.getKey()), toJsonValue(entry.getValue(), context, level));
        }
        return objectBuilder.build();
    }

    private JsonObject toJsonObject(Object object, MapperContext context, int level) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        List<Field> fieldList = MapperContext.findAllFields(object.getClass());
        List<String> includedFieldNames = context.findIncludedFields(object.getClass(), level);
        for (Field field : fieldList) {
            if (!includedFieldNames.isEmpty() && !includedFieldNames.contains(field.getName())) {
                continue;
            }
            Object fieldValue = MapperContext.getFieldValue(object, field);
            if (fieldValue == null) {
                continue;
            }
            objectBuilder.add(field.getName(), toJsonValue(fieldValue, context, level + 1));
        }
        return objectBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private <V> V toJavaObject(JsonValue jsonValue, Class<V> valueType, Class<?> collectionType) {
        if (jsonValue instanceof JsonArray) {
            return toArrayInstance((JsonArray) jsonValue, valueType, collectionType);
        } else if (jsonValue instanceof JsonObject) {
            return toObjectInstance((JsonObject) jsonValue, valueType);
        } else {
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
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <V> V toObjectInstance(JsonObject jsonObject, Class<?> valueType) {
        if (valueType == Object.class || Map.class.isAssignableFrom(valueType)) {// then return a map
            Map<Object, Object> instance = createObjectMapInstance(valueType);
            for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
                instance.put(entry.getKey(), toJavaObject(entry.getValue(), Object.class));
            }
            return (V) Collections.unmodifiableMap(instance);
        } else { // try to create the value type instance
            try {
                V instance = (V) valueType.getDeclaredConstructor().newInstance();
                List<Field> fieldList = MapperContext.findAllFields(valueType);
                for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
                    Field field = null;
                    for (Field f : fieldList) {
                        if (f.getName().equals(entry.getKey())) {
                            field = f;
                            break;
                        }
                    }
                    if (field == null) {
                        continue;//or throw exception here!
                    }
                    Class<?> typeArgument = getTypeArguments(field.getGenericType()).getValue();
                    Object fieldValue = toJavaObject(entry.getValue(), typeArgument, field.getType());
                    MapperContext.setFieldValue(instance, field, fieldValue);
                }
                return instance;
            } catch (Exception e) {
                throw new JsonException("unable to create object instance", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> createObjectMapInstance(Class<?> valueType) {
        if (valueType != Object.class && !valueType.isInterface()) {
            try {
                return (Map<Object, Object>) valueType.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private <V> V toArrayInstance(JsonArray jsonArray, Class<V> valueType, Class<?> collectionType) {
        if (collectionType == null) {
            Map.Entry<Class<?>, Class<?>> typeArguments = getTypeArguments(valueType);
            collectionType = typeArguments.getKey();
            valueType = (Class<V>) typeArguments.getValue();
        }
        if (collectionType.isArray()) {
            V[] instance = (V[]) Array.newInstance(valueType, jsonArray.size());
            for (int i = 0; i < jsonArray.size(); i++) {
                instance[i] = toJavaObject(jsonArray.get(i), valueType);
            }
            return (V) instance;
        } else if (Collection.class.isAssignableFrom(collectionType)) {
            Collection<V> instance = createCollectionInstance(collectionType);
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

    @SuppressWarnings("unchecked")
    private <V> Collection<V> createCollectionInstance(Class<?> collectionType) {
        if (Object.class.equals(collectionType) || List.class.equals(collectionType)) {
            return new ArrayList<>();
        } else if (Set.class.equals(collectionType)) {
            return new HashSet<>();
        } else if (!collectionType.isInterface()) {
            try {
                return (Collection<V>) collectionType.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new JsonException("unable to create collection instance: " + collectionType, e);
            }
        } else {
            throw new JsonException("unable to create collection instance: " + collectionType);
        }
    }

    private static Map.Entry<Class<?>, Class<?>> getTypeArguments(Type type) {
        if (type instanceof Class) {
            return new AbstractMap.SimpleImmutableEntry<>((Class<?>) type, (Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
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
}
