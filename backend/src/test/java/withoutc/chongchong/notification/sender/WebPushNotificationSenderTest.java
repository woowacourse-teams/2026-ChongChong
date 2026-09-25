package withoutc.chongchong.notification.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import withoutc.chongchong.notification.exception.WebPushSendResult;
import withoutc.chongchong.notification.worker.dto.ClaimedDelivery;

@ExtendWith(MockitoExtension.class)
class WebPushNotificationSenderTest {

    private static final String ENDPOINT = "https://push.example.com/subscription";
    private static final String P256DH = "p256dh";
    private static final String AUTH = "auth";

    @Mock
    private WebPushClient webPushClient;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("Web Push payload를 JSON으로 직렬화하고 특수 문자를 보존한다")
    void serializePayload() throws Exception {
        WebPushNotificationSender sender = new WebPushNotificationSender(objectMapper, webPushClient);
        ClaimedDelivery delivery = new ClaimedDelivery(
                1L,
                2L,
                ENDPOINT,
                P256DH,
                AUTH,
                3L,
                "제목 \"테스트\"",
                "본문\n다음 줄",
                "/studies/1/notices/3?tab=detail"
        );
        when(webPushClient.send(eq(ENDPOINT), eq(P256DH), eq(AUTH), anyString())).thenReturn(201);

        WebPushSendResult result = sender.send(delivery);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(webPushClient).send(eq(ENDPOINT), eq(P256DH), eq(AUTH), payloadCaptor.capture());
        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());

        assertThat(result).isEqualTo(WebPushSendResult.SENT);
        assertThat(payload.get("notificationId").asLong()).isEqualTo(3L);
        assertThat(payload.get("title").asText()).isEqualTo("제목 \"테스트\"");
        assertThat(payload.get("body").asText()).isEqualTo("본문\n다음 줄");
        assertThat(payload.get("deepLink").asText()).isEqualTo("/studies/1/notices/3?tab=detail");
    }
}
