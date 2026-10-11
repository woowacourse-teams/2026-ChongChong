package withoutc.chongchong.auth.service;

import withoutc.chongchong.auth.token.IssuedTokenPair;

public record SocialLoginResult(
        Long userId,
        IssuedTokenPair tokenPair
) {

    public SocialLoginResult {
        validateUserId(userId);
        validateTokenPair(tokenPair);
    }

    public static SocialLoginResult of(Long userId, IssuedTokenPair tokenPair) {
        return new SocialLoginResult(userId, tokenPair);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("로그인 사용자 ID는 양수여야 합니다.");
        }
    }

    private void validateTokenPair(IssuedTokenPair tokenPair) {
        if (tokenPair == null) {
            throw new IllegalArgumentException("로그인 발급 Token은 필수입니다.");
        }
    }
}
