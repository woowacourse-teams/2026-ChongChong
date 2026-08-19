package withoutc.chongchong.auth.token;

import java.util.regex.Pattern;

public record HashedRefreshToken(String value) {

    private static final Pattern SHA_256_HEX_PATTERN = Pattern.compile("[0-9a-f]{64}");

    public HashedRefreshToken {
        if (value == null || !SHA_256_HEX_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Refresh Token 해시는 64자리 소문자 16진수여야 합니다.");
        }
    }

    @Override
    public String toString() {
        return "HashedRefreshToken[value=REDACTED]";
    }
}
