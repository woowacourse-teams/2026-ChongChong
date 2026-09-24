package withoutc.chongchong.notification.sender;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import withoutc.chongchong.notification.sender.NotificationEvent.Recipient;

@Component
public class DiscordNotificationSender implements NotificationSender {

    private final RestClient restClient;
    private final String webhookUrl;
    private final String frontendBaseUrl;
    private final List<String> discordUserIds;

    public DiscordNotificationSender(
            RestClient.Builder restClientBuilder,
            @Value("${discord.webhook-url}") String webhookUrl,
            @Value("${frontend.base-url}") String frontendBaseUrl,
            @Value("${discord.user-ids}") List<String> discordUserIds
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();

        this.webhookUrl = webhookUrl;
        this.frontendBaseUrl = frontendBaseUrl;
        this.discordUserIds = discordUserIds;
    }

    @Override
    public void sendNotifications(NotificationEvent event) {
        String message = generateMessage(event);

        String mentions = discordUserIds.stream()
                .map(id -> "<@" + id + ">")
                .collect(Collectors.joining(" "));

        restClient.post()
                .uri(webhookUrl + "?wait=true")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("content", mentions + "\n" + message,
                        "allowed_mentions", Map.of("users", discordUserIds)))
                .retrieve()
                .toBodilessEntity();
    }

    private String generateMessage(NotificationEvent event) {
        List<String> names = event.recipients().stream()
                .map(Recipient::memberName)
                .toList();

        return """
                📢 총총에서 알림이 왔습니다!
                
                %s
                - %s
                
                🐰 총총 바로가기
                %s
                
                대상자: %s
                """
                .formatted(
                        event.title(),
                        event.body(),
                        frontendBaseUrl + event.deepLink(),
                        names
                );
    }
}
