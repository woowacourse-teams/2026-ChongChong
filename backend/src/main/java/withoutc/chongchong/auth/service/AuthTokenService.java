package withoutc.chongchong.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.auth.config.RefreshTokenProperties;
import withoutc.chongchong.auth.entity.AuthSession;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.token.AccessTokenIssuer;
import withoutc.chongchong.auth.token.HashedRefreshToken;
import withoutc.chongchong.auth.token.IssuedAccessToken;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.auth.token.RawRefreshToken;
import withoutc.chongchong.auth.token.RefreshTokenGenerator;
import withoutc.chongchong.auth.token.RefreshTokenHasher;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshTokenProperties refreshTokenProperties;
    private final Clock clock;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public IssuedTokenPair issue(Long userId) {
        validateUserId(userId);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        IssuedTokenPair tokenPair = createTokenPair(userId);
        HashedRefreshToken refreshTokenHash = refreshTokenHasher.hash(tokenPair.refreshToken());

        authSessionRepository.findByUserId(userId)
                .ifPresentOrElse(
                        authSession -> authSession.replaceRefreshToken(
                                refreshTokenHash,
                                tokenPair.refreshTokenExpiresAt()
                        ),
                        () -> authSessionRepository.save(
                                AuthSession.create(user, refreshTokenHash, tokenPair.refreshTokenExpiresAt())
                        )
                );

        return tokenPair;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public IssuedTokenPair rotate(RawRefreshToken currentRefreshToken) {
        validateRefreshToken(currentRefreshToken);
        HashedRefreshToken currentRefreshTokenHash = refreshTokenHasher.hash(currentRefreshToken);
        AuthSession authSession = authSessionRepository
                .findByRefreshTokenHashForUpdate(currentRefreshTokenHash)
                .orElseThrow(this::invalidRefreshToken);

        validateCurrentSession(authSession, currentRefreshTokenHash);

        IssuedTokenPair tokenPair = createTokenPair(authSession.getUser().getId());
        HashedRefreshToken newRefreshTokenHash = refreshTokenHasher.hash(tokenPair.refreshToken());
        authSession.replaceRefreshToken(newRefreshTokenHash, tokenPair.refreshTokenExpiresAt());

        return tokenPair;
    }

    private IssuedTokenPair createTokenPair(Long userId) {
        IssuedAccessToken accessToken = accessTokenIssuer.issue(userId);
        RawRefreshToken refreshToken = refreshTokenGenerator.generate();
        Instant refreshTokenExpiresAt = clock.instant()
                .plus(refreshTokenProperties.validity())
                .truncatedTo(ChronoUnit.MICROS);

        return new IssuedTokenPair(accessToken, refreshToken, refreshTokenExpiresAt);
    }

    private void validateCurrentSession(
            AuthSession authSession,
            HashedRefreshToken currentRefreshTokenHash
    ) {
        if (!authSession.getRefreshTokenHash().equals(currentRefreshTokenHash)
                || authSession.isExpiredAt(clock.instant())) {
            throw invalidRefreshToken();
        }
    }

    private void validateRefreshToken(RawRefreshToken refreshToken) {
        if (refreshToken == null) {
            throw invalidRefreshToken();
        }
    }

    private AuthException invalidRefreshToken() {
        return new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new AuthException(AuthErrorCode.INVALID_USER_ID);
        }
    }
}
