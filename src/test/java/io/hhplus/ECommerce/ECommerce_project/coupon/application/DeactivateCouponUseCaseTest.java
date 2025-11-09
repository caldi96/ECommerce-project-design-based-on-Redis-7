package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DeactivateCouponUseCaseTest {

    private CouponRepository couponRepository;
    private DeactivateCouponUseCase deactivateCouponUseCase;

    @BeforeEach
    void setUp() {
        couponRepository = mock(CouponRepository.class);
        deactivateCouponUseCase = new DeactivateCouponUseCase(couponRepository);
    }

    @Test
    void execute_deactivatesCouponSuccessfully() {
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
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Coupon result = deactivateCouponUseCase.execute(1L);

        // then
        assertThat(result.isActive()).isFalse();

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void execute_couponNotFound_throwsException() {
        // given
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deactivateCouponUseCase.execute(1L))
                .isInstanceOf(CouponException.class)
                .hasMessage(ErrorCode.COUPON_NOT_FOUND.getMessage());
    }
}
