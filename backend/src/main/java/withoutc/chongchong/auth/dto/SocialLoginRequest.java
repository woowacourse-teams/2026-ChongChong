package withoutc.chongchong.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import withoutc.chongchong.auth.social.SocialLoginCommand;
import withoutc.chongchong.auth.social.SocialProvider;

public record SocialLoginRequest(
        @NotNull(message = "소셜 로그인 제공자는 필수입니다.")
        @Schema(description = "소셜 로그인 제공자", example = "KAKAO")
        SocialProvider provider,
        @NotBlank(message = "소셜 로그인 인가 코드는 필수입니다.")
        @Schema(description = "소셜 로그인 인가 코드", example = "authorization-code-from-provider")
        String authorizationCode
) {

    public SocialLoginCommand toCommand() {
        return new SocialLoginCommand(provider, authorizationCode);
    }

    @Override
    public String toString() {
        return "SocialLoginRequest[provider=" + provider + ", authorizationCode=REDACTED]";
    }
}
