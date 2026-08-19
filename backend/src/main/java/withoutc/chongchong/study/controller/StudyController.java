package withoutc.chongchong.study.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.study.dto.MyStudyListResponse;
import withoutc.chongchong.study.dto.StudyCreateRequest;
import withoutc.chongchong.study.dto.StudyCreateResponse;
import withoutc.chongchong.study.dto.StudyInviteLinkResponse;
import withoutc.chongchong.study.service.StudyService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/studies")
public class StudyController {

    private final StudyService studyService;

    @PostMapping
    public ResponseEntity<StudyCreateResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody StudyCreateRequest request
    ) {
        StudyCreateResponse response = studyService.create(user.id(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<MyStudyListResponse> getMyStudies(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        MyStudyListResponse response = studyService.getMyStudies(user.id());

        return ResponseEntity
                .ok(response);
    }

    @GetMapping("/{studyId}/invite-link")
    public ResponseEntity<StudyInviteLinkResponse> getInviteLink(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Positive(message = "스터디 ID는 양수여야 합니다.") Long studyId
    ) {
        StudyInviteLinkResponse response = studyService.getInviteLink(user.id(), studyId);

        return ResponseEntity
                .ok(response);
    }
}
