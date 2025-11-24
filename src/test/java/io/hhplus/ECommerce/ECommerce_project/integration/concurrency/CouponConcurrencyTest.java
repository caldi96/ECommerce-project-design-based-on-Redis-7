package io.hhplus.ECommerce.ECommerce_project.integration.concurrency;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.IssueCouponUseCase;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.IssueCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 쿠폰 동시성 통합 테스트
 *
 * 시나리오:
 * - 제한된 쿠폰을 여러 사용자가 동시에 발급받는 경우
 * - 같은 사용자가 쿠폰을 동시에 여러 번 발급 시도하는 경우
 */
@SpringBootTest
@ActiveProfiles("integration")
public class CouponConcurrencyTest {

    @Autowired
    private IssueCouponUseCase issueCouponUseCase;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("제한된 쿠폰을 여러 사용자가 동시에 발급받을 때 정확히 제한 횟수만큼만 성공해야 한다")
    void testConcurrentCouponIssuanceWithLimit() throws InterruptedException {
        // Given
        int userCount = 20;
        int couponLimit = 10;

        // 쿠폰 생성
        Coupon limitedCoupon = Coupon.createCoupon(
                "제한 쿠폰",
                "LIMITED10",
                DiscountType.FIXED,
                BigDecimal.valueOf(5000),
                null,
                BigDecimal.valueOf(30000),
                couponLimit,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        Coupon savedCoupon = couponRepository.save(limitedCoupon);

        // 사용자들 생성
        User[] users = new User[userCount];
        for (int i = 0; i < userCount; i++) {
            users[i] = new User();
            users[i].setUsername("coupon_user_" + i);
            users[i].setPassword("password");
            users[i].setPointBalance(BigDecimal.ZERO);
            users[i] = userRepository.save(users[i]);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < userCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    IssueCouponCommand command = new IssueCouponCommand(
                            users[userIndex].getId(),
                            savedCoupon.getId()
                    );
                    issueCouponUseCase.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        completionLatch.await();
        executorService.shutdown();

        // Then
        assertThat(successCount.get()).isEqualTo(couponLimit);
        assertThat(failCount.get()).isEqualTo(userCount - couponLimit);

        // 실제 발급된 UserCoupon 개수 확인
        List<UserCoupon> issuedCoupons = userCouponRepository.findAll();
        assertThat(issuedCoupons).hasSize(couponLimit);

        // Coupon의 issuedQuantity 확인
        Coupon finalCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
        assertThat(finalCoupon.getIssuedQuantity()).isEqualTo(couponLimit);
    }

    @Test
    @DisplayName("같은 사용자가 동시에 같은 쿠폰을 여러 번 발급 시도할 때 1번만 성공해야 한다")
    void testSameUserConcurrentDuplicateIssuance() throws InterruptedException {
        // Given
        Coupon coupon = Coupon.createCoupon(
                "중복 발급 테스트 쿠폰",
                "DUP_TEST",
                DiscountType.FIXED,
                BigDecimal.valueOf(3000),
                null,
                BigDecimal.valueOf(20000),
                100,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        Coupon savedCoupon = couponRepository.save(coupon);

        User user = new User();
        user.setUsername("duplicate_user");
        user.setPassword("password");
        user.setPointBalance(BigDecimal.ZERO);
        user = userRepository.save(user);
        final Long userId = user.getId();

        int attemptCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(attemptCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(attemptCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < attemptCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    IssueCouponCommand command = new IssueCouponCommand(
                            userId,
                            savedCoupon.getId()
                    );
                    issueCouponUseCase.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        completionLatch.await();
        executorService.shutdown();

        // Then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(attemptCount - 1);

        // UserCoupon도 1개만 생성되어야 함
        List<UserCoupon> userCoupons = userCouponRepository.findByUser_Id(userId);
        assertThat(userCoupons).hasSize(1);
    }

    @Test
    @DisplayName("여러 사용자가 각각 다른 쿠폰을 동시에 발급받을 때 모두 성공해야 한다")
    void testConcurrentDifferentCouponIssuance() throws InterruptedException {
        // Given
        int userCount = 20;

        // 사용자들 생성
        User[] users = new User[userCount];
        Coupon[] coupons = new Coupon[userCount];

        for (int i = 0; i < userCount; i++) {
            users[i] = new User();
            users[i].setUsername("diff_coupon_user_" + i);
            users[i].setPassword("password");
            users[i].setPointBalance(BigDecimal.ZERO);
            users[i] = userRepository.save(users[i]);

            coupons[i] = Coupon.createCoupon(
                    "쿠폰_" + i,
                    "COUPON_" + i,
                    DiscountType.FIXED,
                    BigDecimal.valueOf(1000),
                    null,
                    BigDecimal.valueOf(10000),
                    100,
                    1,
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now().plusDays(30)
            );
            coupons[i] = couponRepository.save(coupons[i]);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < userCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    IssueCouponCommand command = new IssueCouponCommand(
                            users[index].getId(),
                            coupons[index].getId()
                    );
                    issueCouponUseCase.execute(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        completionLatch.await();
        executorService.shutdown();

        // Then
        assertThat(successCount.get()).isEqualTo(userCount);
        assertThat(failCount.get()).isEqualTo(0);

        // 모든 UserCoupon 생성 확인
        List<UserCoupon> allCoupons = userCouponRepository.findAll();
        assertThat(allCoupons).hasSize(userCount);
    }
}