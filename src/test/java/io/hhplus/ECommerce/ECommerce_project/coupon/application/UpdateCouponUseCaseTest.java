package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.UpdateCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UpdateCouponUseCaseTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private UpdateCouponUseCase updateCouponUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_updatesCouponSuccessfully() {
        // given
        Coupon coupon = Coupon.createCoupon(
                "기존 쿠폰",
                "OLD123",
                DiscountType.FIXED,
                new BigDecimal("1000"),
                null,
                null,
                10,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10)
        );

        UpdateCouponCommand command = new UpdateCouponCommand(
                1L,
                "수정된 쿠폰",
                "NEW123",
                DiscountType.PERCENTAGE,
                new BigDecimal("10"),
                new BigDecimal("5000"),
                new BigDecimal("10000"),
                20,
                2,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().plusDays(20)
        );

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Coupon updated = updateCouponUseCase.execute(command);

        // then
        assertThat(updated.getName()).isEqualTo("수정된 쿠폰");
        assertThat(updated.getCode()).isEqualTo("NEW123");
        assertThat(updated.getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
        assertThat(updated.getDiscountValue()).isEqualTo(new BigDecimal("10"));
        assertThat(updated.getMaxDiscountAmount()).isEqualTo(new BigDecimal("5000"));
        assertThat(updated.getMinOrderAmount()).isEqualTo(new BigDecimal("10000"));
        assertThat(updated.getTotalQuantity()).isEqualTo(20);
        assertThat(updated.getPerUserLimit()).isEqualTo(2);
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void execute_couponNotFound_throwsException() {
        // given
        UpdateCouponCommand command = new UpdateCouponCommand(
                1L, "쿠폰", "CODE", DiscountType.FIXED, new BigDecimal("1000"),
                null, null, 10, 1,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1)
        );

        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> updateCouponUseCase.execute(command))
                .isInstanceOf(CouponException.class)
                .hasMessage(ErrorCode.COUPON_NOT_FOUND.getMessage());
    }
}
