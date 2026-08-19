package withoutc.chongchong.auth.token;

import java.time.Instant;

public record IssuedAccessToken(
        String value,
        Instant expiresAt
) {

    @Override
    public String toString() {
        return "IssuedAccessToken[expiresAt=" + expiresAt + "]";
    }
}
