package withoutc.chongchong.notification.sender;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.notification.entity.NotificationResourceType;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyRepository;

class DiscordNotificationSenderTest {

    private static final String WEBHOOK_URL = "https://discord.test/api/webhooks/1/token";
    private static final String FRONTEND_BASE_URL = "https://test.chongchong.app";

    @Test
    @DisplayName("공지 이벤트를 Discord 웹훅에 한 번 전송하고 대상자와 링크를 포함한다")
    void sendsNoticeEvent() {
        RestClient.Builder mockServerBuilder = RestClient.builder();
        MockRestServiceServer server = bindTo(mockServerBuilder).build();
        RestClient.Builder restClientBuilder = createSenderBuilder(mockServerBuilder);
        StudyRepository studyRepository = mock(StudyRepository.class);
        AssignmentSubmissionRepository submissionRepository = mock(AssignmentSubmissionRepository.class);
        Study study = mock(Study.class);
        when(studyRepository.getByIdOrThrow(3L)).thenReturn(study);
        when(study.getName()).thenReturn("자바 스터디");

        DiscordNotificationSender sender = new DiscordNotificationSender(
                studyRepository,
                submissionRepository,
                restClientBuilder,
                WEBHOOK_URL,
                FRONTEND_BASE_URL,
                List.of("123")
        );
        NotificationEvent event = new NotificationEvent(
                NotificationType.CREATED,
                10L,
                NotificationResourceType.NOTICE,
                3L,
                "공지 제목",
                List.of(new NotificationEvent.Recipient(20L, "멤버"))
        );

        server.expect(requestTo(WEBHOOK_URL + "?wait=true"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.content", containsString("<@123>")))
                .andExpect(jsonPath("$.content", containsString("자바 스터디")))
                .andExpect(jsonPath("$.content", containsString("공지 제목")))
                .andExpect(jsonPath("$.content", containsString("멤버")))
                .andExpect(jsonPath("$.content", containsString(
                        FRONTEND_BASE_URL + "/studies/3/notices/10")))
                .andExpect(jsonPath("$.allowed_mentions.users[0]", is("123")))
                .andRespond(withSuccess());

        sender.sendNotifications(event);

        server.verify();
    }

    @Test
    @DisplayName("제출 이벤트를 Discord 웹훅에 한 번 전송하고 제출자와 제출 링크를 포함한다")
    void sendsAssignmentSubmissionEvent() {
        RestClient.Builder mockServerBuilder = RestClient.builder();
        MockRestServiceServer server = bindTo(mockServerBuilder).build();
        RestClient.Builder restClientBuilder = createSenderBuilder(mockServerBuilder);
        StudyRepository studyRepository = mock(StudyRepository.class);
        AssignmentSubmissionRepository submissionRepository = mock(AssignmentSubmissionRepository.class);
        Study study = mock(Study.class);
        Assignment assignment = mock(Assignment.class);
        AssignmentSubmission submission = mock(AssignmentSubmission.class);
        StudyMember submitter = mock(StudyMember.class);
        when(studyRepository.getByIdOrThrow(3L)).thenReturn(study);
        when(study.getName()).thenReturn("자바 스터디");
        when(submissionRepository.getByIdOrThrow(9L)).thenReturn(submission);
        when(submission.getAssignment()).thenReturn(assignment);
        when(submission.getMember()).thenReturn(submitter);
        when(submitter.getName()).thenReturn("제출자");
        when(assignment.getId()).thenReturn(4L);

        DiscordNotificationSender sender = new DiscordNotificationSender(
                studyRepository,
                submissionRepository,
                restClientBuilder,
                WEBHOOK_URL,
                FRONTEND_BASE_URL,
                List.of("123")
        );
        NotificationEvent event = new NotificationEvent(
                NotificationType.SUBMITTED,
                9L,
                NotificationResourceType.ASSIGNMENT_SUBMISSION,
                3L,
                "제출 내용",
                List.of(new NotificationEvent.Recipient(30L, "리더"))
        );

        server.expect(requestTo(WEBHOOK_URL + "?wait=true"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.content", containsString("<@123>")))
                .andExpect(jsonPath("$.content", containsString("자바 스터디")))
                .andExpect(jsonPath("$.content", containsString("제출자")))
                .andExpect(jsonPath("$.content", containsString("리더")))
                .andExpect(jsonPath("$.content", containsString(
                        FRONTEND_BASE_URL + "/studies/3/assignments/4/submissions/9")))
                .andExpect(jsonPath("$.allowed_mentions.users[0]", is("123")))
                .andRespond(withSuccess());

        sender.sendNotifications(event);

        server.verify();
    }

    private RestClient.Builder createSenderBuilder(RestClient.Builder mockServerBuilder) {
        RestClient mockServerRestClient = mockServerBuilder.build();
        RestClient.Builder senderBuilder = mock(RestClient.Builder.class);
        when(senderBuilder.requestFactory(any(ClientHttpRequestFactory.class))).thenReturn(senderBuilder);
        when(senderBuilder.build()).thenReturn(mockServerRestClient);
        return senderBuilder;
    }
}
