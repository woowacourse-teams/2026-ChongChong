package withoutc.chongchong.auth.exception;

import withoutc.chongchong.global.exception.BusinessException;

public class AuthException extends BusinessException {

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
