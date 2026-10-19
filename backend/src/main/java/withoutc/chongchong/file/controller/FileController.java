package withoutc.chongchong.file.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.file.controller.dto.PresignedUrlCreateRequest;
import withoutc.chongchong.file.controller.dto.PresignedUrlCreateResponse;
import withoutc.chongchong.file.service.FileService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {
    private final FileService fileService;

    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUrlCreateResponse> createPresignedUrl(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody PresignedUrlCreateRequest request) {
        PresignedUrlCreateResponse response = fileService.createPresignedUrl(currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
