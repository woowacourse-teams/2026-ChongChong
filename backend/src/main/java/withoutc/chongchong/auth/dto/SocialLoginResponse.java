package withoutc.chongchong.auth.dto;

import java.time.Instant;

import withoutc.chongchong.auth.service.SocialLoginResult;
import withoutc.chongchong.auth.token.IssuedTokenPair;

public record SocialLoginResponse(
        Long userId,
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt
) {

    private static final String TOKEN_TYPE = "Bearer";

    public static SocialLoginResponse from(SocialLoginResult result) {
        IssuedTokenPair tokenPair = result.tokenPair();
        return new SocialLoginResponse(
                result.userId(),
                TOKEN_TYPE,
                tokenPair.accessToken().value(),
                tokenPair.accessToken().expiresAt()
        );
    }

    @Override
    public String toString() {
        return "SocialLoginResponse[userId=" + userId
                + ", tokenType=" + tokenType
                + ", accessToken=REDACTED, accessTokenExpiresAt=" + accessTokenExpiresAt + "]";
    }
}
