package withoutc.chongchong.auth.token;

import java.time.Instant;

public record IssuedTokenPair(
        IssuedAccessToken accessToken,
        RawRefreshToken refreshToken,
        Instant refreshTokenExpiresAt
) {

    public IssuedTokenPair {
        if (accessToken == null) {
            throw new IllegalArgumentException("발급된 Access Token은 필수입니다.");
        }
        if (refreshToken == null) {
            throw new IllegalArgumentException("발급된 Refresh Token은 필수입니다.");
        }
        if (refreshTokenExpiresAt == null) {
            throw new IllegalArgumentException("Refresh Token 만료 시각은 필수입니다.");
        }
    }

    @Override
    public String toString() {
        return "IssuedTokenPair[accessTokenExpiresAt=" + accessToken.expiresAt()
                + ", refreshToken=REDACTED, refreshTokenExpiresAt=" + refreshTokenExpiresAt + "]";
    }
}
