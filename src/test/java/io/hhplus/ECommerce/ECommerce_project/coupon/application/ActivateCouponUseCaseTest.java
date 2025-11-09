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

class ActivateCouponUseCaseTest {

    private CouponRepository couponRepository;
    private ActivateCouponUseCase activateCouponUseCase;

    @BeforeEach
    void setUp() {
        couponRepository = mock(CouponRepository.class);
        activateCouponUseCase = new ActivateCouponUseCase(couponRepository);
    }

    @Test
    void execute_activatesCouponSuccessfully() {
        // given
        Coupon coupon = Coupon.createCoupon(
                "테스트 쿠폰",
                "TESTCODE",
                DiscountType.FIXED,
                new BigDecimal("1000"),
                null,
                new BigDecimal("10000"),
                100,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10)
        );
        coupon.setId(1L);
        coupon.deactivate(); // 비활성 상태로 시작

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Coupon result = activateCouponUseCase.execute(1L);

        // then
        assertThat(result.isActive()).isTrue();

        // repository save 호출 확인
        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void execute_couponNotFound_throwsException() {
        // given
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> activateCouponUseCase.execute(1L))
                .isInstanceOf(CouponException.class)
                .hasMessage(ErrorCode.COUPON_NOT_FOUND.getMessage());

        verify(couponRepository, never()).save(any());
    }

    @Test
    void execute_alreadyActiveCoupon_throwsException() {
        // given
        Coupon coupon = Coupon.createCoupon(
                "테스트 쿠폰",
                "TESTCODE",
                DiscountType.FIXED,
                new BigDecimal("1000"),
                null,
                new BigDecimal("10000"),
                100,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10)
        );
        coupon.setId(1L); // 이미 활성 상태

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> activateCouponUseCase.execute(1L))
                .isInstanceOf(CouponException.class)
                .hasMessage(ErrorCode.COUPON_ALREADY_ACTIVE.getMessage());

        verify(couponRepository, never()).save(any());
    }
}
