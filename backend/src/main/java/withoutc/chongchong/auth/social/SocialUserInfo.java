package withoutc.chongchong.auth.social;

public record SocialUserInfo(
        SocialProvider provider,
        String providerUserId,
        String displayName,
        String profileImageUrl
) {

    private static final int MAX_PROVIDER_USER_ID_LENGTH = 255;
    private static final int MAX_DISPLAY_NAME_LENGTH = 255;
    private static final int MAX_PROFILE_IMAGE_URL_LENGTH = 2048;

    public SocialUserInfo {
        validateProvider(provider);
        validateProviderUserId(providerUserId);
        validateDisplayName(displayName);
        validateProfileImageUrl(profileImageUrl);
    }

    private void validateProvider(SocialProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("소셜 로그인 제공자는 필수입니다.");
        }
    }

    private void validateProviderUserId(String providerUserId) {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("소셜 제공자 사용자 ID는 비어 있을 수 없습니다.");
        }
        if (providerUserId.length() > MAX_PROVIDER_USER_ID_LENGTH) {
            throw new IllegalArgumentException("소셜 제공자 사용자 ID는 255자를 초과할 수 없습니다.");
        }
    }

    private void validateDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("소셜 사용자 이름은 비어 있을 수 없습니다.");
        }
        if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("소셜 사용자 이름은 255자를 초과할 수 없습니다.");
        }
    }

    private void validateProfileImageUrl(String profileImageUrl) {
        if (profileImageUrl == null) {
            return;
        }
        if (profileImageUrl.isBlank()) {
            throw new IllegalArgumentException("프로필 이미지 URL은 공백일 수 없습니다.");
        }
        if (profileImageUrl.length() > MAX_PROFILE_IMAGE_URL_LENGTH) {
            throw new IllegalArgumentException("프로필 이미지 URL은 2048자를 초과할 수 없습니다.");
        }
    }
}
