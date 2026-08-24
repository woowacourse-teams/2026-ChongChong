package withoutc.chongchong.auth.social.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfoResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {

    @Override
    public String toString() {
        return "KakaoUserInfoResponse[REDACTED]";
    }

    public record KakaoAccount(
            KakaoProfile profile
    ) {

        @Override
        public String toString() {
            return "KakaoAccount[REDACTED]";
        }
    }

    public record KakaoProfile(
            String nickname,
            @JsonProperty("profile_image_url") String profileImageUrl
    ) {

        @Override
        public String toString() {
            return "KakaoProfile[REDACTED]";
        }
    }
}
