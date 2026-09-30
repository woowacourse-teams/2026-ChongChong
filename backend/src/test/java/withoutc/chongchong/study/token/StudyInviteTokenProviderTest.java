package withoutc.chongchong.study.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;

class StudyInviteTokenProviderTest {

    private static final String SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
    private static final String BASE64_URL_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    private StudyInviteTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new StudyInviteTokenProvider(SECRET);
    }

    @Test
    @DisplayName("같은 studyId는 항상 같은 22자 초대 토큰을 생성한다")
    void generateDeterministicTokenTest() {
        String firstToken = provider.generate(1L);
        String secondToken = provider.generate(1L);

        assertThat(firstToken)
                .isEqualTo(secondToken)
                .hasSize(22)
                .matches("[A-Za-z0-9_-]+");
    }

    @Test
    @DisplayName("다른 studyId는 다른 초대 토큰을 생성한다")
    void generateDifferentTokenTest() {
        String firstToken = provider.generate(1L);
        String secondToken = provider.generate(2L);

        assertThat(firstToken).isNotEqualTo(secondToken);
    }

    @Test
    @DisplayName("생성된 토큰에는 원본 studyId가 그대로 노출되지 않는다")
    void generateObfuscatedTokenTest() {
        String token = provider.generate(1L);
        byte[] tokenBytes = Base64.getUrlDecoder().decode(token);
        byte[] studyIdBytes = ByteBuffer.allocate(Long.BYTES)
                .putLong(1L)
                .array();

        assertThat(tokenBytes).isNotEqualTo(studyIdBytes);
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
    @DisplayName("유효한 초대 토큰에서 studyId를 추출한다")
    void verifyAndExtractStudyIdTest() {
        String token = provider.generate(123L);

        Long studyId = provider.verifyAndExtractStudyId(token);

        assertThat(studyId).isEqualTo(123L);
    }

    @Test
    @DisplayName("변조된 초대 토큰은 검증에 실패한다")
    void verifyTamperedTokenTest() {
        String token = provider.generate(123L);
        String tamperedToken = (token.charAt(0) == 'A' ? "B" : "A") + token.substring(1);

        assertInvalidInviteToken(tamperedToken);
    }

    @Test
    @DisplayName("null, 빈 값 또는 잘못된 길이의 초대 토큰은 검증에 실패한다")
    void verifyBlankTokenTest() {
        assertInvalidInviteToken(null);
        assertInvalidInviteToken("");
        assertInvalidInviteToken("   ");
        assertInvalidInviteToken("A".repeat(22));
    }

    @Test
    @DisplayName("비정규 Base64URL 토큰은 검증에 실패한다")
    void verifyNonCanonicalBase64UrlTokenTest() {
        String token = provider.generate(123L);
        int lastCharacterIndex = BASE64_URL_ALPHABET.indexOf(token.charAt(token.length() - 1));
        String nonCanonicalToken = token.substring(0, token.length() - 1)
                + BASE64_URL_ALPHABET.charAt(lastCharacterIndex + 1);

        assertInvalidInviteToken(nonCanonicalToken);
    }

    @Test
    @DisplayName("기존 JWT 초대 토큰은 검증에 실패한다")
    void verifyLegacyJwtTokenTest() {
        assertInvalidInviteToken(
                "eyJhbGciOiJIUzI1NiJ9.eyJwdXJwb3NlIjoic3R1ZHlfam9pbiIsInN0dWR5SWQiOjEyM30."
                        + "aIUwFMzgTqq8dNWEiS_-RqQZcpyVenVAYPR-7CYLe6Q"
        );
    }

    private void assertInvalidInviteToken(String token) {
        assertThatThrownBy(() -> provider.verifyAndExtractStudyId(token))
                .isInstanceOf(StudyException.class)
                .extracting(exception -> ((StudyException) exception).getErrorCode())
                .isEqualTo(StudyErrorCode.INVALID_INVITE_TOKEN);
    }
}
