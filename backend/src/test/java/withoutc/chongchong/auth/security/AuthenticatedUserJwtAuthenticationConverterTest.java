package withoutc.chongchong.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class AuthenticatedUserJwtAuthenticationConverterTest {

    private final AuthenticatedUserJwtAuthenticationConverter converter =
            new AuthenticatedUserJwtAuthenticationConverter();

    @Test
    @DisplayName("JWT subject를 총총 내부 사용자 ID로 변환한다")
    void convertSubjectToAuthenticatedUser() {
        Jwt jwt = jwt("1");

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getPrincipal()).isEqualTo(new AuthenticatedUser(1L));
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("subject가 없는 JWT를 거부한다")
    void rejectMissingSubject() {
        Jwt jwt = jwt(null);

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(InvalidBearerTokenException.class)
                .satisfies(exception -> assertThat(((InvalidBearerTokenException) exception)
                        .getError()
                        .getDescription()).isEqualTo("Invalid access token"));
    }

    @ParameterizedTest(name = "subject={0}")
    @ValueSource(strings = {"abc", "0", "-1"})
    @DisplayName("양의 Long이 아닌 subject를 거부한다")
    void rejectInvalidSubject(String subject) {
        Jwt jwt = jwt(subject);

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(InvalidBearerTokenException.class)
                .satisfies(exception -> assertThat(((InvalidBearerTokenException) exception)
                        .getError()
                        .getDescription()).isEqualTo("Invalid access token"));
    }

    private Jwt jwt(String subject) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("aud", "chongchong-test-api");

        if (subject != null) {
            builder.subject(subject);
        }

        return builder.build();
    }
}
