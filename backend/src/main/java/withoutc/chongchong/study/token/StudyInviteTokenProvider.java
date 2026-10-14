package withoutc.chongchong.study.token;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.fpe.FPEEngine;
import org.bouncycastle.crypto.fpe.FPEFF1Engine;
import org.bouncycastle.crypto.params.FPEParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;

@Component
public class StudyInviteTokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String PURPOSE = "study_join";
    private static final byte[] PURPOSE_BYTES = PURPOSE.getBytes(StandardCharsets.UTF_8);
    private static final int FPE_RADIX = 256;

    private static final int STUDY_ID_BYTE_LENGTH = 8;
    private static final int EXPIRES_AT_BYTE_LENGTH = 4;
    private static final int PAYLOAD_LENGTH = STUDY_ID_BYTE_LENGTH + EXPIRES_AT_BYTE_LENGTH;

    private static final int SIGNATURE_LENGTH = 8;
    private static final int TOKEN_BYTE_LENGTH = PAYLOAD_LENGTH + SIGNATURE_LENGTH;
    private static final int TOKEN_LENGTH = 27;

    private static final Duration INVITE_TOKEN_TTL = Duration.ofDays(3);

    private final SecretKey secretKey;
    private final byte[] secretBytes;
    private final Clock clock;

    public StudyInviteTokenProvider(
            @Value("${jwt.study-invite-secret}") String secret,
            Clock clock
    ) {
        this.secretBytes = Base64.getDecoder().decode(secret);
        this.secretKey = new SecretKeySpec(secretBytes, HMAC_ALGORITHM);
        this.clock = clock;
    }

    public String generate(Long studyId) {
        if (studyId == null || studyId <= 0) {
            throw new StudyException(StudyErrorCode.INVALID_STUDY_ID);
        }

        byte[] payload = generatePayload(studyId);
        byte[] tokenBytes = createTokenBytes(payload);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    private byte[] generatePayload(Long studyId) {
        byte[] studyIdBytes = ByteBuffer.allocate(Long.BYTES)
                .putLong(studyId)
                .array();
        byte[] transformedStudyIdBytes = transformStudyId(studyIdBytes, true);

        long expiresAt = clock.instant()
                .plus(INVITE_TOKEN_TTL)
                .getEpochSecond();

        // 변환한 스터디 ID와 만료 시각을 payload로 직렬화
        return ByteBuffer.allocate(PAYLOAD_LENGTH)
                .put(transformedStudyIdBytes)
                .putInt((int) expiresAt)
                .array();
    }

    private byte[] createTokenBytes(byte[] payload) {
        // HMAC-SHA256 결과 총 32바이트 중 앞 8바이트만 서명으로 사용
        byte[] signature = Arrays.copyOf(sign(payload), SIGNATURE_LENGTH);

        // payload와 서명을 연결해 토큰 바이트 생성
        return ByteBuffer.allocate(TOKEN_BYTE_LENGTH)
                .put(payload)
                .put(signature)
                .array();
    }

    public Long verifyAndExtractStudyId(String token) {
        if (token == null || token.length() != TOKEN_LENGTH) {
            throw invalidInviteToken();
        }

        try {
            byte[] tokenBytes = Base64.getUrlDecoder().decode(token);

            validateCanonicalEncoding(tokenBytes, token);

            byte[] payload = Arrays.copyOf(tokenBytes, PAYLOAD_LENGTH);
            byte[] actualSignature = Arrays.copyOfRange(tokenBytes, PAYLOAD_LENGTH, TOKEN_BYTE_LENGTH);

            validateSignature(payload, actualSignature);
            validateNotExpired(payload);

            return extractStudyId(payload);
        } catch (IllegalArgumentException e) {
            throw invalidInviteToken();
        }
    }

    private byte[] transformStudyId(byte[] studyIdBytes, boolean encrypt) {
        FPEEngine fpe = new FPEFF1Engine();
        // encrypt 값에 따라 스터디 ID를 암호화 또는 복호화
        fpe.init(encrypt, new FPEParameters(new KeyParameter(secretBytes), FPE_RADIX, PURPOSE_BYTES));

        byte[] transformedStudyIdBytes = new byte[studyIdBytes.length];
        fpe.processBlock(studyIdBytes, 0, studyIdBytes.length, transformedStudyIdBytes, 0);
        return transformedStudyIdBytes;
    }

    private byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);
            mac.update(PURPOSE_BYTES);
            return mac.doFinal(payload);
        } catch (GeneralSecurityException e) {
            throw new StudyException(StudyErrorCode.INVITE_TOKEN_SIGN_FAILED);
        }
    }

    private long extractStudyId(byte[] payload) {
        byte[] transformedStudyIdBytes = Arrays.copyOf(payload, STUDY_ID_BYTE_LENGTH);
        byte[] studyIdBytes = transformStudyId(transformedStudyIdBytes, false);
        long studyId = ByteBuffer.wrap(studyIdBytes).getLong();
        if (studyId <= 0) {
            throw invalidInviteToken();
        }
        return studyId;
    }

    private void validateCanonicalEncoding(byte[] tokenBytes, String token) {
        String reEncodedToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
        if (!reEncodedToken.equals(token)) {
            throw invalidInviteToken();
        }
    }

    private void validateSignature(byte[] payload, byte[] actualSignature) {
        // 서명 재생성 후 추출한 서명과 비교
        byte[] expectedSignature = Arrays.copyOf(sign(payload), SIGNATURE_LENGTH);
        if (!MessageDigest.isEqual(actualSignature, expectedSignature)) {
            throw invalidInviteToken();
        }
    }

    private void validateNotExpired(byte[] payload) {
        long expiresAt = Integer.toUnsignedLong(
                ByteBuffer.wrap(payload, STUDY_ID_BYTE_LENGTH, EXPIRES_AT_BYTE_LENGTH).getInt()
        );
        if (clock.instant().getEpochSecond() >= expiresAt) {
            throw invalidInviteToken();
        }
    }

    private StudyException invalidInviteToken() {
        return new StudyException(StudyErrorCode.INVALID_INVITE_TOKEN);
    }
}
