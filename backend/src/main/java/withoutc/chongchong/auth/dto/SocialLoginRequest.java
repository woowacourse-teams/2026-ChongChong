package withoutc.chongchong.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import withoutc.chongchong.auth.social.SocialLoginCommand;
import withoutc.chongchong.auth.social.SocialProvider;

public record SocialLoginRequest(
        @NotNull(message = "소셜 로그인 제공자는 필수입니다.")
        SocialProvider provider,
        @NotBlank(message = "Google ID Token은 필수입니다.")
        String idToken
) {

    public SocialLoginCommand toCommand() {
        return new SocialLoginCommand(provider, idToken);
    }

    @Override
    public String toString() {
        return "SocialLoginRequest[provider=" + provider + ", idToken=REDACTED]";
    }
}
