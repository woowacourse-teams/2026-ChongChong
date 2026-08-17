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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.study.dto.StudyCreateRequest;
import withoutc.chongchong.study.dto.StudyInviteLinkResponse;
import withoutc.chongchong.study.service.StudyService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/studies")
public class StudyController {

    private final StudyService studyService;

    // TODO: 인증, 인가 및 StudyMember 구현 후 현재 사용자 정보 전달
    @PostMapping
    public ResponseEntity<Void> create(
            @Valid @RequestBody StudyCreateRequest request
    ) {
        studyService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    // TODO: 인증, 인가 및 StudyMember 구현 후 현재 사용자 정보 전달
    @GetMapping("/{studyId}/invite-link")
    public ResponseEntity<StudyInviteLinkResponse> getInviteLink(
            @PathVariable @Positive(message = "스터디 ID는 양수여야 합니다.") Long studyId
    ) {
        String inviteLink = studyService.getInviteLink(studyId);
        StudyInviteLinkResponse response = new StudyInviteLinkResponse(inviteLink);

        return ResponseEntity
                .ok(response);
    }
}
