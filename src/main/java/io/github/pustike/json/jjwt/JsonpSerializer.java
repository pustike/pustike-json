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
package io.github.pustike.json.jjwt;

import java.nio.charset.StandardCharsets;
import javax.json.JsonException;

import io.github.pustike.json.ObjectMapper;
import io.github.pustike.json.TypeConverter;
import io.jsonwebtoken.io.SerializationException;
import io.jsonwebtoken.io.Serializer;
import io.jsonwebtoken.lang.Assert;

/**
 * JWT Serializer using JSON-P and object mapper.
 */
public class JsonpSerializer<T> implements Serializer<T> {
    static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper(new TypeConverter());
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unused") //used via ServiceLoader
    public JsonpSerializer() {
        this(DEFAULT_OBJECT_MAPPER);
    }

    @SuppressWarnings({"WeakerAccess", "unused"}) // for end-users providing a custom ObjectMapper
    public JsonpSerializer(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "ObjectMapper cannot be null.");
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        Assert.notNull(t, "Object to serialize cannot be null.");
        try {
            return objectMapper.toJsonValue(t).toString().getBytes(StandardCharsets.UTF_8);
        } catch (JsonException e) {
            throw new SerializationException("Unable to serialize object: " + e.getMessage(), e);
        }
    }
}
