package withoutc.chongchong.study.controller;

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
import withoutc.chongchong.study.dto.StudyInviteTokenRequest;
import withoutc.chongchong.study.service.StudyMemberService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/studies")
public class StudyMemberController {

    private final StudyMemberService studyMemberService;

    @PostMapping("/join")
    public ResponseEntity<Void> join(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody @Valid StudyInviteTokenRequest request
    ) {
        studyMemberService.join(user.id(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }
}
