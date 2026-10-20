package withoutc.chongchong.file.exception;

import withoutc.chongchong.global.exception.BusinessException;

public class FileException extends BusinessException {
    public FileException(FileErrorCode errorCode) {
        super(errorCode);
    }

    public FileException(FileErrorCode errorCode, Throwable cause) {
        super(errorCode);
        initCause(cause);
    }
}
