package withoutc.chongchong.notice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.notice.dto.NoticeCreateRequest;
import withoutc.chongchong.notice.dto.NoticeCreateResponse;
import withoutc.chongchong.notice.service.NoticeService;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@RequiredArgsConstructor
@RequestMapping("/studies/{studyId}/notices")
@RestController
public class NoticeController {

    private static final long MOCK_USER_ID = 1L;

    private final NoticeService noticeService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<NoticeCreateResponse> createNotice(@Valid @RequestBody NoticeCreateRequest request,
                                                             @PathVariable Long studyId) {
        User user = userRepository.findById(MOCK_USER_ID).orElseThrow();
        NoticeCreateResponse response = noticeService.create(user, studyId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
