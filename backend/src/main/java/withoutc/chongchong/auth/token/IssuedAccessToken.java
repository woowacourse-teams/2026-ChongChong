package withoutc.chongchong.auth.token;

import java.time.Instant;

public record IssuedAccessToken(
        String value,
        Instant expiresAt
) {

    public IssuedAccessToken {
        validateValue(value);
        validateExpiresAt(expiresAt);
    }

    private void validateValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("발급된 Access Token 값은 필수입니다.");
        }
    }

    private void validateExpiresAt(Instant expiresAt) {
        if (expiresAt == null) {
            throw new IllegalArgumentException("Access Token 만료 시각은 필수입니다.");
        }
    }

    @Override
    public String toString() {
        return "IssuedAccessToken[expiresAt=" + expiresAt + "]";
    }
}
