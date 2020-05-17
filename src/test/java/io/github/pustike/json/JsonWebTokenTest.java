/*
 * Copyright (c) 2016-2020 the original author or authors.
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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DeserializationException;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.io.SerializationException;
import io.jsonwebtoken.io.Serializer;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * JSON Web Token (JWT) integration tests using jjwt library.
 */
public class JsonWebTokenTest {
    private static Serializer<Map<String, ?>> jwtSerializer;
    private static Deserializer<Map<String, ?>> jwtDeserializer;

    @BeforeAll
    public static void setup() {
        // Setup JWT Serializer/Deserializer implementations using JSON-P and object mapper.
        ObjectMapper objectMapper = new ObjectMapper(new TypeConverter());
        jwtSerializer = map -> {
            Objects.requireNonNull(map, "Object to serialize cannot be null.");
            try {
                return objectMapper.toJsonValue(map).toString().getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new SerializationException("Unable to serialize object: " + e.getMessage(), e);
            }
        };
        jwtDeserializer = bytes -> {
            try {
                return objectMapper.readValue(bytes, Object.class);
            } catch (Exception e) {
                throw new DeserializationException("Unable to deserialize bytes: " + e.getMessage(), e);
            }
        };
    }

    @Test
    public void testPlaintextJwtString() {
        String payload = "{\"iss\":\"joe\",\r\n" +
            " \"exp\":1300819380,\r\n" +
            " \"http://example.com/is_root\":true}";
        String val = Jwts.builder().setPayload(payload).serializeToJsonWith(jwtSerializer).compact();
        String specOutput = "eyJhbGciOiJub25lIn0.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.";
        Assertions.assertEquals(specOutput, val);
    }

    @Test
    public void testJwtAuthString() {
        String secretKeyString = "8gk78CZAfSybKuW34Ls5Esx5AtuUzminZjLmvXl/OgY=";
        final byte[] bytes = Base64.getDecoder().decode(secretKeyString.getBytes(StandardCharsets.UTF_8));
        SecretKey secretKey = Keys.hmacShaKeyFor(bytes);

        String encoded = new String(Base64.getEncoder().encode(secretKey.getEncoded()), StandardCharsets.UTF_8);
        Assertions.assertEquals(secretKeyString, encoded);

        String id = "1";
        Date expiration = Date.from(Instant.now().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS));
        String compact = Jwts.builder().setId(id).setExpiration(expiration).signWith(secretKey)
            .serializeToJsonWith(jwtSerializer).compact();

        Jws<Claims> claimsJws = Jwts.parserBuilder().setSigningKey(secretKey)
                .deserializeJsonWith(jwtDeserializer).build().parseClaimsJws(compact);
        Claims claims = claimsJws.getBody();
        Assertions.assertEquals(id, claims.getId());
        Assertions.assertEquals(expiration, claims.getExpiration());
    }
}
