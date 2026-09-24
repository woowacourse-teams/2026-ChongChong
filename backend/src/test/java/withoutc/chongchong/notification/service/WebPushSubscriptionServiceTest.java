package withoutc.chongchong.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import withoutc.chongchong.notification.controller.dto.WebPushSubscriptionKeysRequest;
import withoutc.chongchong.notification.controller.dto.WebPushSubscriptionRegisterRequest;
import withoutc.chongchong.notification.entity.WebPushSubscription;
import withoutc.chongchong.notification.exception.WebPushSubscriptionErrorCode;
import withoutc.chongchong.notification.exception.WebPushSubscriptionException;
import withoutc.chongchong.notification.repository.WebPushSubscriptionRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.exception.UserErrorCode;
import withoutc.chongchong.user.exception.UserException;
import withoutc.chongchong.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class WebPushSubscriptionServiceTest {

    private static final Long USER_ID = 1L;
    private static final String ENDPOINT = "https://push.example.com/subscription";
    private static final String P256DH = "p256dh-key";
    private static final String AUTH = "auth-secret";

    @Mock
    private WebPushSubscriptionRepository webPushSubscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("존재하는 사용자로 웹 푸시 구독을 등록하면 endpoint 기준으로 upsert한다")
    void registerWebPushSubscription() {
        User user = mock(User.class);
        WebPushSubscription subscription = mock(WebPushSubscription.class);
        when(userRepository.getByIdForUpdateOrThrow(USER_ID)).thenReturn(user);
        when(webPushSubscriptionRepository.upsert(USER_ID, ENDPOINT, P256DH, AUTH)).thenReturn(1);
        when(webPushSubscriptionRepository.findByEndpoint(ENDPOINT)).thenReturn(Optional.of(subscription));
        when(subscription.getId()).thenReturn(10L);

        WebPushSubscriptionService service = new WebPushSubscriptionService(
                webPushSubscriptionRepository,
                userRepository
        );

        var response = service.register(USER_ID, request());

        verify(userRepository).getByIdForUpdateOrThrow(USER_ID);
        verify(webPushSubscriptionRepository).upsert(USER_ID, ENDPOINT, P256DH, AUTH);
        verify(webPushSubscriptionRepository).findByEndpoint(ENDPOINT);
        org.assertj.core.api.Assertions.assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("다른 사용자가 등록한 endpoint면 웹 푸시 구독을 등록하지 않는다")
    void rejectWebPushSubscriptionOwnedByAnotherUser() {
        User user = mock(User.class);
        when(userRepository.getByIdForUpdateOrThrow(USER_ID)).thenReturn(user);
        when(webPushSubscriptionRepository.upsert(USER_ID, ENDPOINT, P256DH, AUTH)).thenReturn(0);

        WebPushSubscriptionService service = new WebPushSubscriptionService(
                webPushSubscriptionRepository,
                userRepository
        );

        assertThatThrownBy(() -> service.register(USER_ID, request()))
                .isInstanceOfSatisfying(WebPushSubscriptionException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(WebPushSubscriptionErrorCode.WEB_PUSH_SUBSCRIPTION_ALREADY_REGISTERED)
                );
        verify(webPushSubscriptionRepository).upsert(USER_ID, ENDPOINT, P256DH, AUTH);
        verifyNoMoreInteractions(webPushSubscriptionRepository);
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 웹 푸시 구독을 저장하지 않는다")
    void rejectMissingUser() {
        UserException exception = new UserException(UserErrorCode.USER_NOT_FOUND);
        when(userRepository.getByIdForUpdateOrThrow(USER_ID)).thenThrow(exception);

        WebPushSubscriptionService service = new WebPushSubscriptionService(
                webPushSubscriptionRepository,
                userRepository
        );

        assertThatThrownBy(() -> service.register(USER_ID, request())).isSameAs(exception);

        verifyNoInteractions(webPushSubscriptionRepository);
    }

    @Test
    @DisplayName("웹 푸시 구독 비활성화는 사용자와 구독 ID를 함께 조건으로 전달한다")
    void deactivateWebPushSubscription() {
        WebPushSubscriptionService service = new WebPushSubscriptionService(
                webPushSubscriptionRepository,
                userRepository
        );

        service.deactivate(USER_ID, 10L);

        verify(webPushSubscriptionRepository).deactivateByIdAndUserId(10L, USER_ID);
        verifyNoInteractions(userRepository);
    }

    private WebPushSubscriptionRegisterRequest request() {
        return new WebPushSubscriptionRegisterRequest(
                ENDPOINT,
                new WebPushSubscriptionKeysRequest(P256DH, AUTH)
        );
    }
}
