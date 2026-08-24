package withoutc.chongchong.auth.social.kakao;

public record KakaoAccessToken(String value) {

    public KakaoAccessToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Kakao Access Token은 비어 있을 수 없습니다.");
        }
        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Kakao Access Token에는 제어 문자를 포함할 수 없습니다.");
        }
    }

    @Override
    public String toString() {
        return "KakaoAccessToken[value=REDACTED]";
    }
}
