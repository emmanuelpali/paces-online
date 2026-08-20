package com.pacesonline.identityservice.auth.refreshtoken;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenGeneratorTest {

    private final RefreshTokenGenerator refreshTokenGenerator =
            new RefreshTokenGenerator();

    @Test
    void generateReturnsUrlSafe256BitToken() {
        String token = refreshTokenGenerator.generate();

        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
    }

    @Test
    void generateReturnsDifferentTokens() {
        String firstToken = refreshTokenGenerator.generate();
        String secondToken = refreshTokenGenerator.generate();

        assertNotEquals(firstToken, secondToken);
    }

    @Test
    void hashReturnsExpectedSha256Digest() {
        String hash = refreshTokenGenerator.hash("test");

        assertEquals(
                "9f86d081884c7d659a2feaa0c55ad015" +
                        "a3bf4f1b2b0b822cd15d6c15b0f00a08",
                hash
        );
    }

    @Test
    void hashReturnsSameDigestForSameToken() {
        String rawToken = refreshTokenGenerator.generate();

        String firstHash = refreshTokenGenerator.hash(rawToken);
        String secondHash = refreshTokenGenerator.hash(rawToken);

        assertEquals(firstHash, secondHash);
        assertEquals(64, firstHash.length());
    }

    @Test
    void hashRejectsNullToken() {
        assertThrows(
                NullPointerException.class,
                () -> refreshTokenGenerator.hash(null)
        );
    }
}