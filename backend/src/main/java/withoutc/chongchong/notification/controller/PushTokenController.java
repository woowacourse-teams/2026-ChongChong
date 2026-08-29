package withoutc.chongchong.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.notification.dto.PushTokenCreateRequest;
import withoutc.chongchong.notification.service.PushTokenService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/push-tokens")
@Tag(name = "Push Token", description = "푸시 토큰 API")
@SecurityRequirement(name = "bearerAuth")
public class PushTokenController {

    private final PushTokenService pushTokenService;

    @PostMapping
    @Operation(summary = "푸시 토큰 등록", description = "현재 사용자의 디바이스 푸시 토큰을 등록한다.")
    @ApiResponse(responseCode = "204", description = "푸시 토큰 등록 성공")
    public ResponseEntity<Void> createPushToken(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody @Valid PushTokenCreateRequest request
    ) {
        pushTokenService.createPushToken(user.id(), request);

        return ResponseEntity
                .noContent()
                .build();
    }
}
