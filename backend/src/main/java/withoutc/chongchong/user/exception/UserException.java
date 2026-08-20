package withoutc.chongchong.user.exception;

import withoutc.chongchong.global.exception.BusinessException;

public class UserException extends BusinessException {

    public UserException(UserErrorCode errorCode) {
        super(errorCode);
    }
}
