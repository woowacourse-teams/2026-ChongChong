package withoutc.chongchong.auth.token;

public record RawRefreshToken(String value) {

    public RawRefreshToken {
        validateValue(value);
    }

    private void validateValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Refresh Token 원문은 비어 있을 수 없습니다.");
        }
    }

    @Override
    public String toString() {
        return "RawRefreshToken[value=REDACTED]";
    }
}
