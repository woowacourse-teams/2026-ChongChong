package withoutc.chongchong.notification.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    INVALID_NOTIFICATION(HttpStatus.BAD_REQUEST, "INVALID_NOTIFICATION",
            "알림의 필수 정보가 올바르지 않습니다."),

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "존재하지 않는 알림입니다."),


    INVALID_NOTIFICATION_DELIVERY(HttpStatus.BAD_REQUEST, "INVALID_NOTIFICATION_DELIVERY",
            "알림 발송 내역의 필수 정보가 올바르지 않습니다."),

    INVALID_NOTIFICATION_DELIVERY_TIME(HttpStatus.BAD_REQUEST, "INVALID_TIME",
            "알림 발송 내역의 시간 정보가 올바르지 않습니다."),

    INVALID_NOTIFICATION_DELIVERY_UPDATE_STATUS(HttpStatus.CONFLICT, "INVALID_NOTIFICATION_DELIVERY_UPDATE_STATUS",
            "알림 발송 내역을 해당 상태로 업데이트할 수 없습니다."),

    NOTIFICATION_DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_DELIVERY_NOT_FOUND", "존재하지 않는 알림 발송 내역입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
