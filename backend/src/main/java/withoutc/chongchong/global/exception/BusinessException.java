package withoutc.chongchong.global.exception;

import lombok.Getter;
import withoutc.chongchong.global.exception.code.ErrorCode;

@Getter
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
