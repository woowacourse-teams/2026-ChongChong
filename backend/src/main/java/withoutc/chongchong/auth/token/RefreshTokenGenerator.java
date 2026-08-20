package withoutc.chongchong.auth.token;

import static withoutc.chongchong.auth.config.RefreshTokenConfig.REFRESH_TOKEN_SECURE_RANDOM;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public final class RefreshTokenGenerator {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final SecureRandom secureRandom;

    public RefreshTokenGenerator(
            @Qualifier(REFRESH_TOKEN_SECURE_RANDOM) SecureRandom secureRandom
    ) {
        this.secureRandom = secureRandom;
    }

    public RawRefreshToken generate() {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);

        String value = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        return new RawRefreshToken(value);
    }
}
