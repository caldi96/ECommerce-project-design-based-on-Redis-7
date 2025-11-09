package io.hhplus.ECommerce.ECommerce_project.coupon.application;

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
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class GetCouponListUseCaseTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private GetCouponListUseCase getCouponListUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_returnsAllCoupons() {
        // given
        Coupon coupon1 = Coupon.createCoupon(
                "쿠폰1",
                "CODE1",
                DiscountType.FIXED,
                new BigDecimal("1000"),
                null,
                null,
                100,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10)
        );

        Coupon coupon2 = Coupon.createCoupon(
                "쿠폰2",
                "CODE2",
                DiscountType.PERCENTAGE,
                new BigDecimal("10"),
                new BigDecimal("5000"),
                null,
                50,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(5)
        );

        when(couponRepository.findAll()).thenReturn(Arrays.asList(coupon1, coupon2));

        // when
        List<Coupon> result = getCouponListUseCase.execute();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(coupon1, coupon2);
    }
}
