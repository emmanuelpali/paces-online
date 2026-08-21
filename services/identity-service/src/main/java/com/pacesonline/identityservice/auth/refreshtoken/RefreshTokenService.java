package com.pacesonline.identityservice.auth.refreshtoken;

import com.pacesonline.identityservice.auth.token.AccessTokenService;
import com.pacesonline.identityservice.config.TokenProperties;
import com.pacesonline.identityservice.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final AccessTokenService accessTokenService;
    private final TokenProperties tokenProperties;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenGenerator refreshTokenGenerator,
            AccessTokenService accessTokenService,
            TokenProperties tokenProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.accessTokenService = accessTokenService;
        this.tokenProperties = tokenProperties;
    }

    @Transactional
    public IssuedRefreshToken issueNewFamily(User user) {
        Instant expiresAt = Instant.now()
                .plus(tokenProperties.refreshTokenExpiration());

        return issueToken(
                user,
                UUID.randomUUID(),
                expiresAt
        );
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshTokenRotationResult rotate(String rawToken) {
        String tokenHash = refreshTokenGenerator.hash(rawToken);

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = Instant.now();

        if (currentToken.isRevoked()
                || currentToken.isExpiredAt(now)) {
            throw new InvalidRefreshTokenException();
        }

        if (currentToken.isConsumed()) {
            refreshTokenRepository.revokeFamilyTokens(
                    currentToken.getFamilyId(),
                    now
            );

            throw new InvalidRefreshTokenException();
        }

        currentToken.consume(now);

        IssuedRefreshToken replacementToken = issueToken(
                currentToken.getUser(),
                currentToken.getFamilyId(),
                currentToken.getExpiresAt()
        );

        String accessToken = accessTokenService.generateAccessToken(
                currentToken.getUser()
        );

        return new RefreshTokenRotationResult(
                accessToken,
                replacementToken.rawToken(),
                replacementToken.expiresAt()
        );
    }

    private IssuedRefreshToken issueToken(
            User user,
            UUID familyId,
            Instant expiresAt
    ) {
        String rawToken = refreshTokenGenerator.generate();
        String tokenHash = refreshTokenGenerator.hash(rawToken);

        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(),
                user,
                familyId,
                tokenHash,
                expiresAt
        );

        refreshTokenRepository.save(refreshToken);

        return new IssuedRefreshToken(
                rawToken,
                expiresAt
        );
    }
}