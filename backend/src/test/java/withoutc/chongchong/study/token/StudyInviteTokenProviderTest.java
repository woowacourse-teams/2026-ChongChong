package withoutc.chongchong.study.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;

class StudyInviteTokenProviderTest {

    private static final String SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    private StudyInviteTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new StudyInviteTokenProvider(SECRET);
    }

    @Test
    @DisplayName("같은 studyId는 항상 같은 JWT를 생성한다")
    void generateDeterministicTokenTest() {
        String firstToken = provider.generate(1L);
        String secondToken = provider.generate(1L);

        assertThat(firstToken).isEqualTo(secondToken);
    }

    @Test
    @DisplayName("JWT에 초대 목적과 studyId를 담는다")
    void generateTokenWithClaimsTest() {
        String token = provider.generate(1L);
        SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));

        Jws<Claims> parsedToken = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);

        assertThat(parsedToken.getPayload().get("purpose", String.class))
                .isEqualTo("study_join");
        assertThat(parsedToken.getPayload().get("studyId", Long.class))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("다른 studyId는 다른 JWT를 생성한다")
    void generateDifferentTokenTest() {
        String firstToken = provider.generate(1L);
        String secondToken = provider.generate(2L);

        assertThat(firstToken).isNotEqualTo(secondToken);
    }

    @Test
    @DisplayName("유효하지 않은 studyId로 토큰을 생성하면 예외가 발생한다")
    void generateInvalidStudyIdTest() {
        assertThatThrownBy(() -> provider.generate(null))
                .isInstanceOf(StudyException.class)
                .extracting(exception -> ((StudyException) exception).getErrorCode())
                .isEqualTo(StudyErrorCode.INVALID_STUDY_ID);

        assertThatThrownBy(() -> provider.generate(0L))
                .isInstanceOf(StudyException.class)
                .extracting(exception -> ((StudyException) exception).getErrorCode())
                .isEqualTo(StudyErrorCode.INVALID_STUDY_ID);

        assertThatThrownBy(() -> provider.generate(-1L))
                .isInstanceOf(StudyException.class)
                .extracting(exception -> ((StudyException) exception).getErrorCode())
                .isEqualTo(StudyErrorCode.INVALID_STUDY_ID);
    }

    @Test
    @DisplayName("유효한 초대 JWT에서 studyId를 추출한다")
    void verifyAndExtractStudyIdTest() {
        String token = provider.generate(123L);

        Long studyId = provider.verifyAndExtractStudyId(token);

        assertThat(studyId).isEqualTo(123L);
    }

    @Test
    @DisplayName("변조된 초대 JWT는 검증에 실패한다")
    void verifyTamperedTokenTest() {
        String token = provider.generate(123L) + "tampered";

        assertInvalidInviteToken(token);
    }

    @Test
    @DisplayName("null 또는 빈 초대 JWT는 검증에 실패한다")
    void verifyBlankTokenTest() {
        assertInvalidInviteToken(null);
        assertInvalidInviteToken("");
        assertInvalidInviteToken("   ");
    }

    @Test
    @DisplayName("초대 목적이 다른 JWT는 검증에 실패한다")
    void verifyWrongPurposeTokenTest() {
        String token = Jwts.builder()
                .claim("purpose", "other_purpose")
                .claim("studyId", 123L)
                .signWith(secretKey(), Jwts.SIG.HS256)
                .compact();

        assertInvalidInviteToken(token);
    }

    @Test
    @DisplayName("studyId가 없는 JWT는 검증에 실패한다")
    void verifyMissingStudyIdTokenTest() {
        String token = Jwts.builder()
                .claim("purpose", "study_join")
                .signWith(secretKey(), Jwts.SIG.HS256)
                .compact();

        assertInvalidInviteToken(token);
    }

    @Test
    @DisplayName("유효하지 않은 studyId를 담은 JWT는 검증에 실패한다")
    void verifyInvalidStudyIdTokenTest() {
        String token = Jwts.builder()
                .claim("purpose", "study_join")
                .claim("studyId", 0L)
                .signWith(secretKey(), Jwts.SIG.HS256)
                .compact();

        assertInvalidInviteToken(token);
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    private void assertInvalidInviteToken(String token) {
        assertThatThrownBy(() -> provider.verifyAndExtractStudyId(token))
                .isInstanceOf(StudyException.class)
                .extracting(exception -> ((StudyException) exception).getErrorCode())
                .isEqualTo(StudyErrorCode.INVALID_INVITE_TOKEN);
    }
}
