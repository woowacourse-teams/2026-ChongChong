package withoutc.chongchong.auth.support;

import static io.restassured.RestAssured.given;

import io.restassured.specification.RequestSpecification;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public final class TestAuthRequest {

    private final TestJwtFactory testJwtFactory;

    public TestAuthRequest(TestJwtFactory testJwtFactory) {
        this.testJwtFactory = testJwtFactory;
    }

    public RequestSpecification givenAuthenticatedUser(Long userId) {
        return givenAccessToken(testJwtFactory.accessToken(userId));
    }

    public RequestSpecification givenAccessToken(String accessToken) {
        return given().auth().oauth2(accessToken);
    }
}
