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
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.sender.NotificationEvent.Recipient;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.repository.StudyRepository;

@Component
public class DiscordNotificationSender implements NotificationSender {

    private final StudyRepository studyRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    private final RestClient restClient;
    private final String webhookUrl;
    private final String frontendBaseUrl;
    private final List<String> discordUserIds;

    public DiscordNotificationSender(
            StudyRepository studyRepository,
            AssignmentSubmissionRepository submissionRepository,
            RestClient.Builder restClientBuilder,
            @Value("${discord.webhook-url}") String webhookUrl,
            @Value("${frontend.base-url}") String frontendBaseUrl,
            @Value("${discord.user-ids}") List<String> discordUserIds
    ) {
        this.studyRepository = studyRepository;
        this.submissionRepository = submissionRepository;

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
        String link = generateLink(event.studyId(), event.resourceId(), event.resourceType());
        String message = generateMessage(event, link);

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

    private String generateMessage(NotificationEvent event, String link) {
        List<String> names = event.recipients().stream()
                .map(Recipient::name)
                .toList();

        String content = event.content();

        if (event.resourceType() == ResourceType.ASSIGNMENT_SUBMISSION) {
            AssignmentSubmission submission = submissionRepository.getByIdOrThrow(event.resourceId());
            content = String.format("%s 스터디원이 과제를 제출했어요", submission.getMember().getName());
        }

        Study study = studyRepository.getByIdOrThrow(event.studyId());

        return """
                📢 총총에서 알림이 왔습니다!
                
                [%s] 새 %s
                - %s
                
                🐰 총총 바로가기
                %s
                
                대상자: %s
                """
                .formatted(
                        study.getName(),
                        event.resourceType().name,
                        content,
                        link,
                        names
                );
    }

    private String generateLink(Long studyId, Long resourceId, ResourceType resourceType) {
        return switch (resourceType) {
            case NOTICE -> frontendBaseUrl + "/studies/" + studyId + "/notices/" + resourceId;
            case ASSIGNMENT -> frontendBaseUrl + "/studies/" + studyId + "/assignments/" + resourceId;
            case ASSIGNMENT_SUBMISSION -> assignmentSubmissionLink(studyId, resourceId);
        };
    }

    private String assignmentSubmissionLink(Long studyId, Long resourceId) {
        AssignmentSubmission submission = submissionRepository.getByIdOrThrow(resourceId);
        return frontendBaseUrl + "/studies/" + studyId + "/assignments/" + submission.getAssignment().getId()
                + "/submissions/" + resourceId;
    }
}
