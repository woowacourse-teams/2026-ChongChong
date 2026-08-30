package withoutc.chongchong.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.notification.dto.PushTokenCreateRequest;

@Tag(name = "Push Token", description = "푸시 토큰 API")
@SecurityRequirement(name = "bearerAuth")
public interface PushTokenApi {

    @Operation(summary = "푸시 토큰 등록", description = "현재 사용자의 디바이스 푸시 토큰을 등록한다.")
    @ApiResponse(responseCode = "204", description = "푸시 토큰 등록 성공")
    ResponseEntity<Void> createPushToken(
            AuthenticatedUser user,
            @Valid PushTokenCreateRequest request
    );
}
