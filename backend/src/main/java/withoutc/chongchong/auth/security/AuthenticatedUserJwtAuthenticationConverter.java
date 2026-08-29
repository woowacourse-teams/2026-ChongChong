package withoutc.chongchong.auth.security;

import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class AuthenticatedUserJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String INVALID_ACCESS_TOKEN_MESSAGE = "Invalid access token";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(parseUserId(jwt.getSubject()));

        return new JwtAuthenticationToken(jwt, authenticatedUser, List.of());
    }

    private Long parseUserId(String subject) {
        if (subject == null) {
            throw new InvalidBearerTokenException(INVALID_ACCESS_TOKEN_MESSAGE);
        }

        try {
            long userId = Long.parseLong(subject);

            if (userId <= 0) {
                throw new InvalidBearerTokenException(INVALID_ACCESS_TOKEN_MESSAGE);
            }

            return userId;
        } catch (NumberFormatException e) {
            throw new InvalidBearerTokenException(INVALID_ACCESS_TOKEN_MESSAGE, e);
        }
    }
}
