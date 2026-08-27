package withoutc.chongchong.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import withoutc.chongchong.auth.token.IssuedTokenPair;

public record SocialLoginResponse(
        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,
        @Schema(description = "API 요청에 사용할 액세스 토큰")
        String accessToken,
        @Schema(description = "액세스 토큰 만료 시각", example = "2026-08-27T12:00:00Z")
        Instant accessTokenExpiresAt
) {

    private static final String TOKEN_TYPE = "Bearer";

    public static SocialLoginResponse from(IssuedTokenPair tokenPair) {
        return new SocialLoginResponse(
                TOKEN_TYPE,
                tokenPair.accessToken().value(),
                tokenPair.accessToken().expiresAt()
        );
    }

    @Override
    public String toString() {
        return "SocialLoginResponse[tokenType=" + tokenType
                + ", accessToken=REDACTED, accessTokenExpiresAt=" + accessTokenExpiresAt + "]";
    }
}
