package io.hhplus.ECommerce.ECommerce_project.user.domain.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import org.springframework.stereotype.Component;

@Component
public class UserDomainService {

    /**
     * ID 값이 유효한지 검증
     */
    public void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new UserException(ErrorCode.USER_ID_INVALID);
        }
    }
}
