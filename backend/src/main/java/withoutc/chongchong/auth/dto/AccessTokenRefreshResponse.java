package withoutc.chongchong.auth.dto;

import withoutc.chongchong.auth.token.IssuedTokenPair;

import java.time.Instant;

public record AccessTokenRefreshResponse(
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt
) {

    private static final String TOKEN_TYPE = "Bearer";

    public static AccessTokenRefreshResponse from(IssuedTokenPair tokenPair) {
        return new AccessTokenRefreshResponse(
                TOKEN_TYPE,
                tokenPair.accessToken().value(),
                tokenPair.accessToken().expiresAt()
        );
    }

    @Override
    public String toString() {
        return "AccessTokenRefreshResponse[tokenType=" + tokenType
                + ", accessToken=REDACTED, accessTokenExpiresAt=" + accessTokenExpiresAt + "]";
    }
}
