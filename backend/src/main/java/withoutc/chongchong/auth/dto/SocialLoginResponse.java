package withoutc.chongchong.auth.dto;

import java.time.Instant;
import withoutc.chongchong.auth.token.IssuedTokenPair;

public record SocialLoginResponse(
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {

    private static final String TOKEN_TYPE = "Bearer";

    public static SocialLoginResponse from(IssuedTokenPair tokenPair) {
        return new SocialLoginResponse(
                TOKEN_TYPE,
                tokenPair.accessToken().value(),
                tokenPair.accessToken().expiresAt(),
                tokenPair.refreshToken().value(),
                tokenPair.refreshTokenExpiresAt()
        );
    }

    @Override
    public String toString() {
        return "SocialLoginResponse[tokenType=" + tokenType
                + ", accessToken=REDACTED, accessTokenExpiresAt=" + accessTokenExpiresAt
                + ", refreshToken=REDACTED, refreshTokenExpiresAt=" + refreshTokenExpiresAt + "]";
    }
}
