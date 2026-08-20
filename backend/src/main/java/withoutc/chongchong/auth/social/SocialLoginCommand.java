package withoutc.chongchong.auth.social;

public record SocialLoginCommand(
        SocialProvider provider,
        String authorizationCode
) {

    public SocialLoginCommand {
        validateProvider(provider);
        validateAuthorizationCode(authorizationCode);
    }

    private void validateProvider(SocialProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("소셜 로그인 제공자는 필수입니다.");
        }
    }

    private void validateAuthorizationCode(String authorizationCode) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new IllegalArgumentException("Authorization Code는 비어 있을 수 없습니다.");
        }
    }

    @Override
    public String toString() {
        return "SocialLoginCommand[provider=" + provider + ", authorizationCode=REDACTED]";
    }
}
