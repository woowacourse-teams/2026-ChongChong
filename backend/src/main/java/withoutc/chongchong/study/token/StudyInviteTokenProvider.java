package withoutc.chongchong.study.token;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
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
    private static final int SIGNATURE_LENGTH = 8;
    private static final int TOKEN_BYTE_LENGTH = Long.BYTES + SIGNATURE_LENGTH;
    private static final int TOKEN_LENGTH = 22;

    private final SecretKey secretKey;
    private final byte[] secretBytes;

    public StudyInviteTokenProvider(
            @Value("${jwt.study-invite-secret}") String secret
    ) {
        this.secretBytes = Base64.getDecoder().decode(secret);
        this.secretKey = new SecretKeySpec(secretBytes, HMAC_ALGORITHM);
    }

    public String generate(Long studyId) {
        if (studyId == null || studyId <= 0) {
            throw new StudyException(StudyErrorCode.INVALID_STUDY_ID);
        }

        byte[] studyIdBytes = ByteBuffer.allocate(Long.BYTES)
                .putLong(studyId)
                .array();
        byte[] transformedStudyIdBytes = transformStudyId(studyIdBytes, true);
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];

        // [ 섞인 studyId 8바이트 ][ 서명 8바이트 ]
        System.arraycopy(transformedStudyIdBytes, 0, tokenBytes, 0, Long.BYTES);
        System.arraycopy(sign(transformedStudyIdBytes), 0, tokenBytes, Long.BYTES, SIGNATURE_LENGTH);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    public Long verifyAndExtractStudyId(String token) {
        if (token == null || token.length() != TOKEN_LENGTH) {
            throw invalidInviteToken();
        }

        try {
            byte[] tokenBytes = Base64.getUrlDecoder().decode(token);
            String reEncodedToken = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(tokenBytes);
            if (!reEncodedToken.equals(token)) {
                throw invalidInviteToken();
            }

            byte[] transformedStudyIdBytes = Arrays.copyOf(tokenBytes, Long.BYTES);
            byte[] actualSignature = Arrays.copyOfRange(tokenBytes, Long.BYTES, TOKEN_BYTE_LENGTH);

            // 서명 다시 생성 후 추출한 서명과 비교
            byte[] expectedSignature = Arrays.copyOf(sign(transformedStudyIdBytes), SIGNATURE_LENGTH);
            if (!MessageDigest.isEqual(actualSignature, expectedSignature)) {
                throw invalidInviteToken();
            }

            byte[] studyIdBytes = transformStudyId(transformedStudyIdBytes, false);
            long studyId = ByteBuffer.wrap(studyIdBytes).getLong();
            if (studyId <= 0) {
                throw invalidInviteToken();
            }
            return studyId;
        } catch (IllegalArgumentException e) {
            throw invalidInviteToken();
        }
    }

    private byte[] transformStudyId(byte[] studyIdBytes, boolean encrypt) {
        FPEEngine fpe = new FPEFF1Engine();
        fpe.init(encrypt, new FPEParameters(new KeyParameter(secretBytes), FPE_RADIX, PURPOSE_BYTES));

        byte[] transformedStudyIdBytes = new byte[studyIdBytes.length];
        fpe.processBlock(studyIdBytes, 0, studyIdBytes.length, transformedStudyIdBytes, 0);
        return transformedStudyIdBytes;
    }

    private byte[] sign(byte[] studyIdBytes) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);
            mac.update(PURPOSE_BYTES);
            return mac.doFinal(studyIdBytes);
        } catch (GeneralSecurityException e) {
            throw new StudyException(StudyErrorCode.INVITE_TOKEN_SIGN_FAILED);
        }
    }

    private StudyException invalidInviteToken() {
        return new StudyException(StudyErrorCode.INVALID_INVITE_TOKEN);
    }
}
