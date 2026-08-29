package withoutc.chongchong.auth.social.kakao;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import withoutc.chongchong.auth.config.KakaoLoginProperties;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.social.kakao.dto.KakaoTokenResponse;

public class KakaoTokenClient {

    private static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";

    private final RestClient restClient;
    private final KakaoLoginProperties properties;

    public KakaoTokenClient(
            RestClient restClient,
            KakaoLoginProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public KakaoAccessToken exchange(String authorizationCode) {
        validateAuthorizationCode(authorizationCode);

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(createTokenRequest(authorizationCode))
                    .retrieve()
                    .body(KakaoTokenResponse.class);
            return toAccessToken(response);
        } catch (RestClientException | IllegalArgumentException exception) {
            throw authenticationFailed();
        }
    }

    private MultiValueMap<String, String> createTokenRequest(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", AUTHORIZATION_CODE_GRANT_TYPE);
        form.add("client_id", properties.restApiKey());
        form.add("redirect_uri", properties.redirectUri().toString());
        form.add("code", authorizationCode);
        form.add("client_secret", properties.clientSecret());
        return form;
    }

    private KakaoAccessToken toAccessToken(KakaoTokenResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("Kakao Token 응답은 필수입니다.");
        }
        return response.toAccessToken();
    }

    private void validateAuthorizationCode(String authorizationCode) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw authenticationFailed();
        }
    }

    private AuthException authenticationFailed() {
        return new AuthException(AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED);
    }
}
