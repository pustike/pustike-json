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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtTest {
    @Test
    public void testPlaintextJwtString() {
        String payload = "{\"iss\":\"joe\",\r\n" +
            " \"exp\":1300819380,\r\n" +
            " \"http://example.com/is_root\":true}";
        String val = Jwts.builder().setPayload(payload).compact();
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
        Instant expiration = Instant.now().plus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        String compact = Jwts.builder().setId(id).setExpiration(expiration).signWith(secretKey).compact();

        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(compact);
        Claims claims = claimsJws.getBody();
        Assertions.assertEquals(id, claims.getId());
        Assertions.assertEquals(expiration, claims.getExpiration());
    }
}
