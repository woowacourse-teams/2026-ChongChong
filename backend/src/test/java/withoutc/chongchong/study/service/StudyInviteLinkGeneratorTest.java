package withoutc.chongchong.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import withoutc.chongchong.study.token.StudyInviteTokenProvider;

@ExtendWith(MockitoExtension.class)
class StudyInviteLinkGeneratorTest {

    private static final String FRONTEND_BASE_URL = "https://test.chongchong.app";

    @Mock
    private StudyInviteTokenProvider studyInviteTokenProvider;

    private StudyInviteLinkGenerator studyInviteLinkGenerator;

    @BeforeEach
    void setUp() {
        studyInviteLinkGenerator = new StudyInviteLinkGenerator(
                studyInviteTokenProvider,
                FRONTEND_BASE_URL
        );
    }

    @Test
    @DisplayName("스터디 ID로 초대 링크를 생성한다")
    void generateTest() {
        Long studyId = 1L;
        when(studyInviteTokenProvider.generate(studyId)).thenReturn("invite-token");

        String inviteLink = studyInviteLinkGenerator.generate(studyId);

        assertThat(inviteLink).isEqualTo("https://test.chongchong.app/studies/join?token=invite-token");
        verify(studyInviteTokenProvider).generate(studyId);
    }
}
