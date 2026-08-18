package withoutc.chongchong.notice.controller;

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

    private NoticeService noticeService;
    private UserRepository userRepository;

    private final User mockUser = User.create("바니", "https://avatars.githubusercontent.com/u/156324288?v=4&size=64");

    @PostMapping
    public ResponseEntity<NoticeCreateResponse> createNotice(@RequestBody NoticeCreateRequest request,
                                                             @PathVariable Long studyId) {
        User user = userRepository.save(mockUser);
        NoticeCreateResponse response = noticeService.create(user, studyId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
