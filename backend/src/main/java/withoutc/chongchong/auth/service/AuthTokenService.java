package withoutc.chongchong.auth.service;

import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

    @Transactional
    public IssuedTokenPair issue(Long userId) {
        validateUserId(userId);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        IssuedAccessToken accessToken = accessTokenIssuer.issue(userId);
        RawRefreshToken refreshToken = refreshTokenGenerator.generate();
        HashedRefreshToken refreshTokenHash = refreshTokenHasher.hash(refreshToken);
        Instant refreshTokenExpiresAt = clock.instant().plus(refreshTokenProperties.validity());

        authSessionRepository.findByUserId(userId)
                .ifPresentOrElse(
                        authSession -> authSession.replaceRefreshToken(refreshTokenHash, refreshTokenExpiresAt),
                        () -> authSessionRepository.save(
                                AuthSession.create(user, refreshTokenHash, refreshTokenExpiresAt)
                        )
                );

        return new IssuedTokenPair(accessToken, refreshToken, refreshTokenExpiresAt);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
        }
    }
}
