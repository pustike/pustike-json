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

import javax.json.JsonException;

import io.github.pustike.json.ObjectMapper;
import io.jsonwebtoken.io.DeserializationException;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.lang.Assert;

/**
 * JWT Deserializer using JSON-P and object mapper.
 */
public class JsonpDeserializer<T> implements Deserializer<T> {
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unused") //used via ServiceLoader
    public JsonpDeserializer() {
        this(JsonpSerializer.DEFAULT_OBJECT_MAPPER);
    }

    @SuppressWarnings({"WeakerAccess", "unused"}) // for end-users providing a custom ObjectMapper
    public JsonpDeserializer(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "ObjectMapper cannot be null.");
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(byte[] bytes) throws DeserializationException {
        try {
            return objectMapper.readValue(bytes, (Class<T>) Object.class);
        } catch (JsonException e) {
            throw new DeserializationException("Unable to deserialize bytes: " + e.getMessage(), e);
        }
    }
}
