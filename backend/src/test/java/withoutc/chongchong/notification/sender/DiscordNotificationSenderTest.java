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
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.ResourceType;

class DiscordNotificationSenderTest {

    private static final String WEBHOOK_URL = "https://discord.test/api/webhooks/1/token";
    private static final String FRONTEND_BASE_URL = "https://test.chongchong.app";

    @Test
    @DisplayName("공지 이벤트를 Discord 웹훅에 한 번 전송하고 대상자와 링크를 포함한다")
    void sendsNoticeEvent() {
        RestClient.Builder mockServerBuilder = RestClient.builder();
        MockRestServiceServer server = bindTo(mockServerBuilder).build();
        RestClient.Builder restClientBuilder = createSenderBuilder(mockServerBuilder);

        DiscordNotificationSender sender = new DiscordNotificationSender(
                restClientBuilder,
                WEBHOOK_URL,
                FRONTEND_BASE_URL,
                List.of("123")
        );
        NotificationEvent event = new NotificationEvent(
                "[자바 스터디] 새 공지",
                "공지 제목",
                NotificationType.NEW,
                10L,
                ResourceType.NOTICE,
                "/studies/3/notices/10",
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

        DiscordNotificationSender sender = new DiscordNotificationSender(
                restClientBuilder,
                WEBHOOK_URL,
                FRONTEND_BASE_URL,
                List.of("123")
        );
        NotificationEvent event = new NotificationEvent(
                "[자바 스터디] 새 제출물",
                "제출자 스터디원이 과제를 제출했어요",
                NotificationType.NEW,
                9L,
                ResourceType.ASSIGNMENT_SUBMISSION,
                "/studies/3/assignments/4/submissions/9",
                List.of(new NotificationEvent.Recipient(30L, "리더"))
        );

        server.expect(requestTo(WEBHOOK_URL + "?wait=true"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.content", containsString("<@123>")))
                .andExpect(jsonPath("$.content", containsString("자바 스터디")))
                .andExpect(jsonPath("$.content", containsString("제출자 스터디원이 과제를 제출했어요")))
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
