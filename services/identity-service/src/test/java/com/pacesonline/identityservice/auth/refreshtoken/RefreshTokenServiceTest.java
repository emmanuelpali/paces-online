package com.pacesonline.identityservice.auth.refreshtoken;

import com.pacesonline.identityservice.auth.token.AccessTokenService;
import com.pacesonline.identityservice.config.TokenProperties;
import com.pacesonline.identityservice.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Duration ACCESS_TOKEN_EXPIRATION =
            Duration.ofMinutes(15);

    private static final Duration REFRESH_TOKEN_EXPIRATION =
            Duration.ofDays(7);

    private static final String RAW_TOKEN = "raw-refresh-token";
    private static final String TOKEN_HASH = "a".repeat(64);

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private AccessTokenService accessTokenService;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        TokenProperties tokenProperties = new TokenProperties(
                "paces-online-test",
                ACCESS_TOKEN_EXPIRATION,
                REFRESH_TOKEN_EXPIRATION,
                null,
                null
        );

        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                refreshTokenGenerator,
                accessTokenService,
                tokenProperties
        );
    }

    @Test
    void issueNewFamilyPersistsHashAndReturnsRawToken() {
        User user = createUser();
        Instant earliestExpiration =
                Instant.now().plus(REFRESH_TOKEN_EXPIRATION);

        when(refreshTokenGenerator.generate()).thenReturn(RAW_TOKEN);
        when(refreshTokenGenerator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);

        IssuedRefreshToken result =
                refreshTokenService.issueNewFamily(user);

        Instant latestExpiration =
                Instant.now().plus(REFRESH_TOKEN_EXPIRATION);

        ArgumentCaptor<RefreshToken> tokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken persistedToken = tokenCaptor.getValue();

        assertEquals(RAW_TOKEN, result.rawToken());
        assertSame(user, persistedToken.getUser());
        assertNotNull(persistedToken.getFamilyId());
        assertEquals(TOKEN_HASH, persistedToken.getTokenHash());
        assertNotEquals(RAW_TOKEN, persistedToken.getTokenHash());
        assertEquals(result.expiresAt(), persistedToken.getExpiresAt());

        assertFalse(result.expiresAt().isBefore(earliestExpiration));
        assertFalse(result.expiresAt().isAfter(latestExpiration));
    }

    @Test
    void rotateConsumesCurrentTokenAndIssuesNewTokenPair() {
        User user = createUser();
        UUID familyId = UUID.randomUUID();
        Instant familyExpiration = Instant.now().plus(Duration.ofDays(3));

        RefreshToken currentToken = new RefreshToken(
                UUID.randomUUID(),
                user,
                familyId,
                TOKEN_HASH,
                familyExpiration
        );

        String replacementRawToken = "replacement-refresh-token";
        String replacementHash = "b".repeat(64);
        String accessToken = "new-access-token";

        when(refreshTokenGenerator.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);

        when(refreshTokenRepository.findByTokenHashForUpdate(TOKEN_HASH))
                .thenReturn(Optional.of(currentToken));

        when(refreshTokenGenerator.generate())
                .thenReturn(replacementRawToken);

        when(refreshTokenGenerator.hash(replacementRawToken))
                .thenReturn(replacementHash);

        when(accessTokenService.generateAccessToken(user))
                .thenReturn(accessToken);

        RefreshTokenRotationResult result =
                refreshTokenService.rotate(RAW_TOKEN);

        ArgumentCaptor<RefreshToken> tokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken replacementToken = tokenCaptor.getValue();

        assertTrue(currentToken.isConsumed());
        assertEquals(accessToken, result.accessToken());
        assertEquals(replacementRawToken, result.refreshToken());
        assertEquals(familyExpiration, result.refreshTokenExpiresAt());

        assertSame(user, replacementToken.getUser());
        assertEquals(familyId, replacementToken.getFamilyId());
        assertEquals(replacementHash, replacementToken.getTokenHash());
        assertEquals(familyExpiration, replacementToken.getExpiresAt());
        assertNotEquals(currentToken.getId(), replacementToken.getId());

        verify(refreshTokenRepository, never())
                .revokeFamilyTokens(any(), any());
    }

    @Test
    void rotateRejectsUnknownToken() {
        when(refreshTokenGenerator.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);

        when(refreshTokenRepository.findByTokenHashForUpdate(TOKEN_HASH))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(RAW_TOKEN)
        );

        verify(refreshTokenRepository, never()).save(any());
        verify(accessTokenService, never())
                .generateAccessToken(any());
    }

    @Test
    void rotateRejectsExpiredToken() {
        RefreshToken expiredToken = new RefreshToken(
                UUID.randomUUID(),
                createUser(),
                UUID.randomUUID(),
                TOKEN_HASH,
                Instant.now().minusSeconds(1)
        );

        arrangeExistingToken(expiredToken);

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(RAW_TOKEN)
        );

        verify(refreshTokenRepository, never()).save(any());
        verify(accessTokenService, never())
                .generateAccessToken(any());
    }

    @Test
    void rotateRejectsRevokedToken() {
        RefreshToken revokedToken = createActiveToken();
        revokedToken.revoke(Instant.now());

        arrangeExistingToken(revokedToken);

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(RAW_TOKEN)
        );

        verify(refreshTokenRepository, never()).save(any());
        verify(accessTokenService, never())
                .generateAccessToken(any());
    }

    @Test
    void rotateRevokesFamilyWhenConsumedTokenIsReused() {
        RefreshToken consumedToken = createActiveToken();
        consumedToken.consume(Instant.now().minusSeconds(1));

        arrangeExistingToken(consumedToken);

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(RAW_TOKEN)
        );

        verify(refreshTokenRepository).revokeFamilyTokens(
                eq(consumedToken.getFamilyId()),
                any(Instant.class)
        );

        verify(refreshTokenRepository, never()).save(any());
        verify(accessTokenService, never())
                .generateAccessToken(any());
    }

    private void arrangeExistingToken(RefreshToken refreshToken) {
        when(refreshTokenGenerator.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);

        when(refreshTokenRepository.findByTokenHashForUpdate(TOKEN_HASH))
                .thenReturn(Optional.of(refreshToken));
    }

    private RefreshToken createActiveToken() {
        return new RefreshToken(
                UUID.randomUUID(),
                createUser(),
                UUID.randomUUID(),
                TOKEN_HASH,
                Instant.now().plus(Duration.ofDays(1))
        );
    }

    private User createUser() {
        return new User(
                UUID.randomUUID(),
                "runner@example.com",
                "encoded-password"
        );
    }
}