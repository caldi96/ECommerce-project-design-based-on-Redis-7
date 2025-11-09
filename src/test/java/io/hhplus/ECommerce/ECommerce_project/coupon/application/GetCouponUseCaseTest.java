package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class GetCouponUseCaseTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private GetCouponUseCase getCouponUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_returnsCouponSuccessfully() {
        // given
        Coupon coupon = Coupon.createCoupon(
                "테스트 쿠폰",
                "TEST123",
                DiscountType.FIXED,
                new BigDecimal("1000"),
                null,
                null,
                100,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10)
        );

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        // when
        Coupon result = getCouponUseCase.execute(1L);

        // then
        assertThat(result).isEqualTo(coupon);
    }

    @Test
    void execute_nonExistingCoupon_throwsException() {
        // given
        when(couponRepository.findById(999L)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> getCouponUseCase.execute(999L))
                .isInstanceOf(CouponException.class)
                .hasMessage(ErrorCode.COUPON_NOT_FOUND.getMessage());
    }
}
