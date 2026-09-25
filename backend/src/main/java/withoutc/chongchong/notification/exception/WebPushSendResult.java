package withoutc.chongchong.notification.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WebPushSendResult {
    SENT("전송 완료"),
    SUBSCRIPTION_EXPIRED("웹 푸시 구독 만료"),
    RETRYABLE_FAILURE("재시도 가능한 발송 실패"),
    PERMANENT_FAILURE("영구적 빌송 실패");

    private final String message;
}
