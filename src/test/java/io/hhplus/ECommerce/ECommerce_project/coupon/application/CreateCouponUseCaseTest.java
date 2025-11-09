package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.CreateCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CreateCouponUseCaseTest {

    private CouponRepository couponRepository;
    private CreateCouponUseCase createCouponUseCase;

    @BeforeEach
    void setUp() {
        couponRepository = mock(CouponRepository.class);
        createCouponUseCase = new CreateCouponUseCase(couponRepository);
    }

    @Test
    void execute_createsCouponSuccessfully() {
        // given
        CreateCouponCommand command = new CreateCouponCommand(
                "테스트 쿠폰",
                "TEST123",
                DiscountType.FIXED,
                new BigDecimal("1000"),
                null,
                new BigDecimal("10000"),
                100,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10)
        );

        when(couponRepository.save(any(Coupon.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Coupon coupon = createCouponUseCase.execute(command);

        // then
        assertThat(coupon.getName()).isEqualTo("테스트 쿠폰");
        assertThat(coupon.getCode()).isEqualTo("TEST123");
        assertThat(coupon.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(coupon.getDiscountValue()).isEqualTo(new BigDecimal("1000"));
        assertThat(coupon.getTotalQuantity()).isEqualTo(100);
        assertThat(coupon.getPerUserLimit()).isEqualTo(1);
        assertThat(coupon.isActive()).isTrue();

        // save 호출 확인
        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("테스트 쿠폰");
    }

    @Test
    void execute_invalidCoupon_throwsException() {
        // 예: 총 수량이 0이면 CouponException 발생
        CreateCouponCommand command = new CreateCouponCommand(
                "테스트 쿠폰",
                "TEST123",
                DiscountType.FIXED,
                new BigDecimal("1000"),
                null,
                new BigDecimal("10000"),
                0,  // 잘못된 총 수량
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10)
        );

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> createCouponUseCase.execute(command))
                .isInstanceOf(RuntimeException.class);  // CouponException
    }
}
