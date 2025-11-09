package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.IssueCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.UserCouponStatus;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.UserCouponRepository;
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
import static org.mockito.Mockito.*;

class IssueCouponUseCaseTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private IssueCouponUseCase issueCouponUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_issuesCouponSuccessfully() {
        // given
        IssueCouponCommand command = new IssueCouponCommand(1L, 100L);

        Coupon coupon = Coupon.createCoupon(
                "테스트 쿠폰",
                "TEST100",
                DiscountType.FIXED,
                new BigDecimal("1000"),
                null,
                null,
                10,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10)
        );

        when(couponRepository.findById(100L)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.findByUserIdAndCouponId(1L, 100L)).thenReturn(Optional.empty());
        when(couponRepository.increaseIssuedQuantityWithLock(100L)).thenReturn(coupon);
        when(userCouponRepository.save(any(UserCoupon.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserCoupon result = issueCouponUseCase.execute(command);

        // then
        assertThat(result.getCouponId()).isEqualTo(100L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
        verify(userCouponRepository).save(any(UserCoupon.class));
        verify(couponRepository).increaseIssuedQuantityWithLock(100L);
    }

    @Test
    void execute_alreadyIssued_throwsException() {
        // given
        IssueCouponCommand command = new IssueCouponCommand(1L, 100L);
        Coupon coupon = Coupon.createCoupon(
                "테스트 쿠폰",
                "TEST100",
                DiscountType.FIXED,
                new BigDecimal("1000"),
                null,
                null,
                10,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10)
        );

        when(couponRepository.findById(100L)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.findByUserIdAndCouponId(1L, 100L))
                .thenReturn(Optional.of(UserCoupon.issueCoupon(1L, 100L)));

        // then
        assertThatThrownBy(() -> issueCouponUseCase.execute(command))
                .isInstanceOf(CouponException.class)
                .hasMessage("이미 발급받은 쿠폰입니다.");
    }

    @Test
    void execute_couponAllIssued_throwsException() {
        // given
        IssueCouponCommand command = new IssueCouponCommand(1L, 100L);
        Coupon coupon = Coupon.createCoupon(
                "테스트 쿠폰",
                "TEST100",
                DiscountType.FIXED,
                new BigDecimal("1000"),
                null,
                null,
                10,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10)
        );

        when(couponRepository.findById(100L)).thenReturn(Optional.of(coupon));
        when(userCouponRepository.findByUserIdAndCouponId(1L, 100L)).thenReturn(Optional.empty());
        when(couponRepository.increaseIssuedQuantityWithLock(100L)).thenReturn(null); // 수량 증가 실패

        // then
        assertThatThrownBy(() -> issueCouponUseCase.execute(command))
                .isInstanceOf(CouponException.class)
                .hasMessage(ErrorCode.COUPON_NOT_FOUND.getMessage());
    }
}
