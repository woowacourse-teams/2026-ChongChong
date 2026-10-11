package withoutc.chongchong.study.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;

class StudyInviteTokenProviderTest {

    private static final String SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
    private static final Instant ISSUED_AT = Instant.parse("2026-10-07T00:00:00Z");
    private static final Instant EXPIRES_AT = ISSUED_AT.plus(Duration.ofDays(3));
    private static final int TOKEN_LENGTH = 27;
    private static final String BASE64_URL_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    private StudyInviteTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = providerAt(ISSUED_AT);
    }

    @Test
    @DisplayName("같은 studyId와 시각은 같은 27자 초대 토큰을 생성한다")
    void generateDeterministicTokenTest() {
        String firstToken = provider.generate(1L);
        String secondToken = provider.generate(1L);

        assertThat(firstToken)
                .isEqualTo(secondToken)
                .hasSize(TOKEN_LENGTH)
                .matches("[A-Za-z0-9_-]+");
    }

    @Test
    @DisplayName("초대 토큰에 스터디 ID, 3일 뒤 만료 시각, 서명이 포함된다")
    void generateTokenWithExpirationTest() {
        byte[] tokenBytes = Base64.getUrlDecoder().decode(provider.generate(1L));
        long expiresAt = Integer.toUnsignedLong(
                ByteBuffer.wrap(tokenBytes, Long.BYTES, Integer.BYTES).getInt()
        );

        assertThat(tokenBytes).hasSize(20);
        assertThat(expiresAt).isEqualTo(EXPIRES_AT.getEpochSecond());
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

        assertThat(Arrays.copyOf(tokenBytes, Long.BYTES)).isNotEqualTo(studyIdBytes);
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
    @DisplayName("만료 직전까지는 초대 토큰을 사용할 수 있다")
    void verifyInviteTokenBeforeExpirationTest() {
        String token = provider.generate(123L);
        StudyInviteTokenProvider providerBeforeExpiration = providerAt(EXPIRES_AT.minusSeconds(1));

        Long studyId = providerBeforeExpiration.verifyAndExtractStudyId(token);

        assertThat(studyId).isEqualTo(123L);
    }

    @Test
    @DisplayName("만료 시각부터 초대 토큰을 사용할 수 없다")
    void verifyExpiredInviteTokenTest() {
        String token = provider.generate(123L);

        assertInvalidInviteToken(providerAt(EXPIRES_AT), token);
    }

    @Test
    @DisplayName("변조된 초대 토큰은 검증에 실패한다")
    void verifyTamperedTokenTest() {
        String token = provider.generate(123L);
        String tamperedToken = (token.charAt(0) == 'A' ? "B" : "A") + token.substring(1);

        assertInvalidInviteToken(tamperedToken);
    }

    @Test
    @DisplayName("만료 시각이 변조된 초대 토큰은 검증에 실패한다")
    void verifyTamperedExpirationTest() {
        byte[] tokenBytes = Base64.getUrlDecoder().decode(provider.generate(123L));
        tokenBytes[Long.BYTES] ^= 1;
        String tamperedToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);

        assertInvalidInviteToken(tamperedToken);
    }

    @Test
    @DisplayName("null, 빈 값 또는 잘못된 길이의 초대 토큰은 검증에 실패한다")
    void verifyBlankTokenTest() {
        assertInvalidInviteToken(null);
        assertInvalidInviteToken("");
        assertInvalidInviteToken("   ");
        assertInvalidInviteToken("A".repeat(TOKEN_LENGTH - 1));
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
        assertInvalidInviteToken(provider, token);
    }

    private void assertInvalidInviteToken(StudyInviteTokenProvider tokenProvider, String token) {
        assertThatThrownBy(() -> tokenProvider.verifyAndExtractStudyId(token))
                .isInstanceOf(StudyException.class)
                .extracting(exception -> ((StudyException) exception).getErrorCode())
                .isEqualTo(StudyErrorCode.INVALID_INVITE_TOKEN);
    }

    private StudyInviteTokenProvider providerAt(Instant instant) {
        return new StudyInviteTokenProvider(SECRET, Clock.fixed(instant, ZoneOffset.UTC));
    }
}
