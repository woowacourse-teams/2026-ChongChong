package withoutc.chongchong.global.exception.code;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    HttpStatus getHttpStatus();

    String getCode();

    String getMessage();
}
