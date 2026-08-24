package withoutc.chongchong.auth.social.kakao;

import java.net.URI;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import withoutc.chongchong.auth.config.KakaoLoginProperties;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.social.kakao.dto.KakaoUserInfoResponse;

public class KakaoUserInfoClient {

    private final RestClient restClient;
    private final KakaoLoginProperties properties;

    public KakaoUserInfoClient(
            RestClient restClient,
            KakaoLoginProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public KakaoUserInfoResponse fetch(KakaoAccessToken accessToken) {
        validateAccessToken(accessToken);

        try {
            return restClient.get()
                    .uri(createUserInfoUri())
                    .headers(headers -> headers.setBearerAuth(accessToken.value()))
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);
        } catch (RestClientException exception) {
            throw authenticationFailed();
        }
    }

    private URI createUserInfoUri() {
        return UriComponentsBuilder.fromUri(properties.userInfoUri())
                .queryParam("secure_resource", true)
                .build(true)
                .toUri();
    }

    private void validateAccessToken(KakaoAccessToken accessToken) {
        if (accessToken == null) {
            throw authenticationFailed();
        }
    }

    private AuthException authenticationFailed() {
        return new AuthException(AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED);
    }
}
