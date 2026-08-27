package withoutc.chongchong.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import withoutc.chongchong.notification.dto.PushTokenCreateRequest;
import withoutc.chongchong.notification.entity.DevicePlatform;
import withoutc.chongchong.notification.entity.PushToken;
import withoutc.chongchong.notification.entity.TokenProvider;
import withoutc.chongchong.notification.repository.PushTokenRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PushTokenServiceTest {

    private static final Long USER_ID = 1L;
    private static final String INSTALLATION_ID = "installation-1";
    private static final String TOKEN = "push-token";

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("푸시 토큰이 없으면 설치 식별자와 함께 저장한다")
    void createPushTokenTest() {
        User user = User.create("총총이", null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(pushTokenRepository.findByUserIdAndInstallationId(USER_ID, INSTALLATION_ID))
                .thenReturn(Optional.empty());

        PushTokenService pushTokenService = new PushTokenService(pushTokenRepository, userRepository);

        pushTokenService.createPushToken(USER_ID, request());

        ArgumentCaptor<PushToken> captor = ArgumentCaptor.forClass(PushToken.class);
        verify(pushTokenRepository).saveAndFlush(captor.capture());
        PushToken saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getInstallationId()).isEqualTo(INSTALLATION_ID);
        assertThat(saved.getProvider()).isEqualTo(TokenProvider.EXPO);
        assertThat(saved.getToken()).isEqualTo(TOKEN);
        assertThat(saved.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("같은 사용자와 설치 식별자로 다시 요청하면 저장하지 않는다")
    void createPushTokenAgainTest() {
        User user = User.create("총총이", null);
        PushToken existing = PushToken.create(
                user, INSTALLATION_ID, TokenProvider.EXPO, TOKEN, DevicePlatform.ANDROID
        );
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(pushTokenRepository.findByUserIdAndInstallationId(USER_ID, INSTALLATION_ID))
                .thenReturn(Optional.of(existing));

        PushTokenService pushTokenService = new PushTokenService(pushTokenRepository, userRepository);

        pushTokenService.createPushToken(USER_ID, request());

        verify(pushTokenRepository, never()).saveAndFlush(any(PushToken.class));
        assertThat(existing.isActive()).isTrue();
    }

    @Test
    @DisplayName("저장 중 데이터 무결성 예외가 발생하면 정상 종료한다")
    void ignoreDataIntegrityViolationTest() {
        User user = User.create("총총이", null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(pushTokenRepository.findByUserIdAndInstallationId(USER_ID, INSTALLATION_ID))
                .thenReturn(Optional.empty());
        when(pushTokenRepository.saveAndFlush(any(PushToken.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        PushTokenService pushTokenService = new PushTokenService(pushTokenRepository, userRepository);

        assertThatCode(() -> pushTokenService.createPushToken(USER_ID, request()))
                .doesNotThrowAnyException();
    }

    private PushTokenCreateRequest request() {
        return new PushTokenCreateRequest(
                INSTALLATION_ID,
                TokenProvider.EXPO,
                TOKEN,
                DevicePlatform.ANDROID
        );
    }
}
