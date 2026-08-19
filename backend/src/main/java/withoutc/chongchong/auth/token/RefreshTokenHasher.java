package withoutc.chongchong.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public final class RefreshTokenHasher {

    private static final String HASH_ALGORITHM = "SHA-256";

    public HashedRefreshToken hash(RawRefreshToken rawRefreshToken) {
        if (rawRefreshToken == null) {
            throw new IllegalArgumentException("Refresh Token 원문은 필수입니다.");
        }

        byte[] hashBytes = messageDigest().digest(rawRefreshToken.value().getBytes(StandardCharsets.UTF_8));
        String hashValue = HexFormat.of().formatHex(hashBytes);

        return new HashedRefreshToken(hashValue);
    }

    private MessageDigest messageDigest() {
        try {
            return MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
