package withoutc.chongchong.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import withoutc.chongchong.notification.controller.dto.PushTokenCreateRequest;
import withoutc.chongchong.notification.entity.DevicePlatform;
import withoutc.chongchong.notification.entity.TokenProvider;
import withoutc.chongchong.notification.repository.PushTokenRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.exception.UserErrorCode;
import withoutc.chongchong.user.exception.UserException;
import withoutc.chongchong.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PushTokenServiceTest {

    private static final Long USER_ID = 1L;
    private static final String INSTALLATION_ID = "installation-1";

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("존재하는 사용자로 푸시 토큰을 등록하면 EXPO 정보와 함께 upsert한다")
    void registerPushToken() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(userRepository.getByIdForUpdateOrThrow(USER_ID)).thenReturn(user);

        PushTokenService pushTokenService = new PushTokenService(pushTokenRepository, userRepository);

        pushTokenService.registerPushToken(
                USER_ID,
                new PushTokenCreateRequest(INSTALLATION_ID, "push-token", DevicePlatform.ANDROID)
        );

        verify(userRepository).getByIdForUpdateOrThrow(USER_ID);
        verify(pushTokenRepository).upsert(
                USER_ID,
                INSTALLATION_ID,
                TokenProvider.EXPO.name(),
                "push-token",
                DevicePlatform.ANDROID.name()
        );
    }

    @Test
    @DisplayName("기존 설치 식별자로 다시 등록하면 새 토큰과 플랫폼을 upsert한다")
    void updatePushTokenRegistration() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(userRepository.getByIdForUpdateOrThrow(USER_ID)).thenReturn(user);

        PushTokenService pushTokenService = new PushTokenService(pushTokenRepository, userRepository);

        pushTokenService.registerPushToken(
                USER_ID,
                new PushTokenCreateRequest(INSTALLATION_ID, "rotated-token", DevicePlatform.IOS)
        );

        verify(pushTokenRepository).upsert(
                USER_ID,
                INSTALLATION_ID,
                TokenProvider.EXPO.name(),
                "rotated-token",
                DevicePlatform.IOS.name()
        );
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 푸시 토큰을 저장하지 않는다")
    void rejectMissingUser() {
        UserException exception = new UserException(UserErrorCode.USER_NOT_FOUND);
        when(userRepository.getByIdForUpdateOrThrow(USER_ID)).thenThrow(exception);

        PushTokenService pushTokenService = new PushTokenService(pushTokenRepository, userRepository);

        assertThatThrownBy(() -> pushTokenService.registerPushToken(
                USER_ID,
                new PushTokenCreateRequest(INSTALLATION_ID, "push-token", DevicePlatform.ANDROID)
        )).isSameAs(exception);

        verifyNoInteractions(pushTokenRepository);
    }

    @Test
    @DisplayName("푸시 토큰 비활성화는 사용자와 설치 식별자를 함께 조건으로 전달한다")
    void deactivatePushToken() {
        PushTokenService pushTokenService = new PushTokenService(pushTokenRepository, userRepository);

        pushTokenService.deactivatePushToken(USER_ID, INSTALLATION_ID);

        verify(pushTokenRepository).deactivateByInstallationIdAndUserId(INSTALLATION_ID, USER_ID);
        verifyNoInteractions(userRepository);
    }
}
