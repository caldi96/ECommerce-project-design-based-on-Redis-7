package io.hhplus.ECommerce.ECommerce_project.common.exception;

public class LockException extends BusinessException{

    public LockException(ErrorCode errorCode) {
        super(errorCode);
    }

    public LockException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
