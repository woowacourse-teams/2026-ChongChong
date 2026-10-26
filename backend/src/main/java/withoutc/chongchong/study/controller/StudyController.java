package withoutc.chongchong.study.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.auth.security.AuthenticatedUser;
import withoutc.chongchong.global.pagination.CursorPageRequest;
import withoutc.chongchong.study.controller.dto.MyStudyListResponse;
import withoutc.chongchong.study.controller.dto.StudyCreateRequest;
import withoutc.chongchong.study.controller.dto.StudyCreateResponse;
import withoutc.chongchong.study.controller.dto.StudyDetailResponse;
import withoutc.chongchong.study.controller.dto.StudyInfoResponse;
import withoutc.chongchong.study.controller.dto.StudyInviteLinkResponse;
import withoutc.chongchong.study.controller.dto.StudyListResponse;
import withoutc.chongchong.study.controller.dto.StudySummaryResponse;
import withoutc.chongchong.study.controller.dto.StudyUpdateRequest;
import withoutc.chongchong.study.service.StudyService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/studies")
public class StudyController {

    private final StudyService studyService;

    @PostMapping
    public ResponseEntity<StudyCreateResponse> createStudy(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody @Valid StudyCreateRequest request
    ) {
        StudyCreateResponse response = studyService.createStudy(user.id(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{studyId}")
    public ResponseEntity<Void> updateStudy(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable
            @Positive(message = "스터디 ID는 양수여야 합니다.")
            Long studyId,
            @RequestBody @Valid StudyUpdateRequest request
    ) {
        studyService.updateStudy(user.id(), studyId, request);

        return ResponseEntity
                .noContent()
                .build();
    }

    @DeleteMapping("/{studyId}")
    public ResponseEntity<Void> deleteStudy(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable
            @Positive(message = "스터디 ID는 양수여야 합니다.")
            Long studyId
    ) {
        studyService.deleteStudy(user.id(), studyId);
        return ResponseEntity
                .noContent()
                .build();
    }

    @GetMapping("/{studyId}")
    public ResponseEntity<StudyDetailResponse> getStudyDetail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable
            @Positive(message = "스터디 ID는 양수여야 합니다.")
            Long studyId
    ) {
        StudyDetailResponse response = studyService.getStudyDetail(user.id(), studyId);
        return ResponseEntity
                .ok(response);
    }

    @GetMapping("/{studyId}/info")
    public ResponseEntity<StudyInfoResponse> getStudyInfo(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable
            @Positive(message = "스터디 ID는 양수여야 합니다.")
            Long studyId
    ) {
        StudyInfoResponse response = studyService.getStudyInfo(user.id(), studyId);
        return ResponseEntity
                .ok(response);
    }


    @GetMapping("/me")
    public ResponseEntity<MyStudyListResponse> getMyStudies(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        MyStudyListResponse response = studyService.getMyStudies(user.id());

        return ResponseEntity
                .ok(response);
    }

    @GetMapping("/public")
    public ResponseEntity<StudyListResponse> getPublicStudies(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false)
            @Positive(message = "cursor는 양수여야 합니다.")
            Long cursor,
            @RequestParam(defaultValue = "10")
            @Positive(message = "size는 양수여야 합니다.")
            @Max(value = CursorPageRequest.MAX_SIZE, message = "size는 100 이하여야 합니다.")
            int size
    ) {
        StudyListResponse response = studyService.getPublicStudies(user.id(), cursor, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/search")
    public ResponseEntity<StudyListResponse> searchPublicStudies(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam
            @NotBlank(message = "검색어는 필수입니다.")
            @Size(min = 1, max = 15, message = "검색어는 1자 이상 15자 이하여야 합니다.")
            String keyword,
            @RequestParam(required = false)
            @Positive(message = "cursor는 양수여야 합니다.")
            Long cursor,
            @RequestParam(defaultValue = "10")
            @Positive(message = "size는 양수여야 합니다.")
            @Max(value = CursorPageRequest.MAX_SIZE, message = "size는 100 이하여야 합니다.")
            int size
    ) {
        StudyListResponse response = studyService.searchPublicStudies(user.id(), keyword.strip(), cursor, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/join-info")
    public ResponseEntity<StudySummaryResponse> getStudyJoinInfo(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false)
            @Positive(message = "스터디 ID는 양수여야 합니다.")
            Long studyId,
            @RequestParam(required = false) String token
    ) {
        StudySummaryResponse response = studyService.getStudyJoinInfo(user.id(), studyId, token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{studyId}/invite-link")
    public ResponseEntity<StudyInviteLinkResponse> getInviteLink(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable
            @Positive(message = "스터디 ID는 양수여야 합니다.")
            Long studyId
    ) {
        StudyInviteLinkResponse response = studyService.getInviteLink(user.id(), studyId);

        return ResponseEntity
                .ok(response);
    }
}
