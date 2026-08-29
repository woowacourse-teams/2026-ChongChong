package withoutc.chongchong.auth.social.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import withoutc.chongchong.auth.social.kakao.KakaoAccessToken;

public record KakaoTokenResponse(
        @JsonProperty("access_token") String accessToken
) {

    public KakaoAccessToken toAccessToken() {
        return new KakaoAccessToken(accessToken);
    }

    @Override
    public String toString() {
        return "KakaoTokenResponse[accessToken=REDACTED]";
    }
}
