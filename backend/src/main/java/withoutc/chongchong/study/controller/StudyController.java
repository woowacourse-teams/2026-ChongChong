package withoutc.chongchong.study.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.study.dto.StudyCreateRequest;
import withoutc.chongchong.study.dto.StudyCreateResponse;
import withoutc.chongchong.study.dto.StudyInviteLinkResponse;
import withoutc.chongchong.study.service.StudyService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/studies")
public class StudyController {

    private final StudyService studyService;

    // TODO: 인증, 인가 및 StudyMember 구현 후 현재 사용자 정보 전달
    @PostMapping
    public ResponseEntity<StudyCreateResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody StudyCreateRequest request
    ) {
        StudyCreateResponse response = studyService.create(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // TODO: 인증, 인가 및 StudyMember 구현 후 현재 사용자 정보 전달
    @GetMapping("/{studyId}/invite-link")
    public ResponseEntity<StudyInviteLinkResponse> getInviteLink(
            @PathVariable @Positive(message = "스터디 ID는 양수여야 합니다.") Long studyId
    ) {
        StudyInviteLinkResponse response = studyService.getInviteLink(studyId);

        return ResponseEntity
                .ok(response);
    }
}
