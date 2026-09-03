package withoutc.chongchong.study.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import withoutc.chongchong.study.token.StudyInviteTokenProvider;

@Component
public class StudyInviteLinkGenerator {

    private final StudyInviteTokenProvider studyInviteTokenProvider;
    private final String frontendBaseUrl;

    public StudyInviteLinkGenerator(
            StudyInviteTokenProvider studyInviteTokenProvider,
            @Value("${frontend.base-url}") String frontendBaseUrl
    ) {
        this.studyInviteTokenProvider = studyInviteTokenProvider;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public String generate(Long studyId) {
        return UriComponentsBuilder
                .fromUriString(frontendBaseUrl)
                .path("/studies/join")
                .queryParam("token", studyInviteTokenProvider.generate(studyId))
                .build()
                .toUriString();
    }
}
