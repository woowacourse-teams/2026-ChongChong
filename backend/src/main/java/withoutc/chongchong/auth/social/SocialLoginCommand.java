package withoutc.chongchong.auth.social;

public record SocialLoginCommand(
        SocialProvider provider,
        String credential
) {

    public SocialLoginCommand {
        validateProvider(provider);
        validateCredential(credential);
    }

    private void validateProvider(SocialProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("소셜 로그인 제공자는 필수입니다.");
        }
    }

    private void validateCredential(String credential) {
        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException("소셜 로그인 인증 정보는 비어 있을 수 없습니다.");
        }
    }

    @Override
    public String toString() {
        return "SocialLoginCommand[provider=" + provider + ", credential=REDACTED]";
    }
}
