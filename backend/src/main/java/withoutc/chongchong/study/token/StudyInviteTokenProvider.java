package withoutc.chongchong.study.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;

@Component
public class StudyInviteTokenProvider {

    private static final String PURPOSE = "study_join";

    private final SecretKey secretKey;

    public StudyInviteTokenProvider(
            @Value("${jwt.study-invite-secret}") String secret
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generate(Long studyId) {
        if (studyId == null || studyId <= 0) {
            throw new StudyException(StudyErrorCode.INVALID_STUDY_ID);
        }

        return Jwts.builder()
                .claim("purpose", PURPOSE)
                .claim("studyId", studyId)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Long verifyAndExtractStudyId(String token) {
        try {
            Jws<Claims> parsedToken = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = parsedToken.getPayload();
            String purpose = claims.get("purpose", String.class);

            if (!PURPOSE.equals(purpose)) {
                throw new StudyException(StudyErrorCode.INVALID_INVITE_TOKEN);
            }

            Long studyId = claims.get("studyId", Long.class);
            if (studyId == null || studyId <= 0) {
                throw new StudyException(StudyErrorCode.INVALID_INVITE_TOKEN);
            }

            return studyId;
        } catch (JwtException | IllegalArgumentException e) {
            throw new StudyException(StudyErrorCode.INVALID_INVITE_TOKEN);
        }
    }
}
