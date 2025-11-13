package io.hhplus.ECommerce.ECommerce_project.integration.concurrency;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.IssueCouponUseCase;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.IssueCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.UserCouponMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.CreateOrderFromProductUseCase;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 쿠폰 동시성 통합 테스트 (A 방식: 선착순 쿠폰 발급)
 *
 * 시나리오:
 * - 제한된 쿠폰을 여러 사용자가 동시에 발급받는 경우
 * - 발급받은 쿠폰을 사용하여 주문하는 경우
 */
@SpringBootTest
public class CouponConcurrencyTest {

    @Autowired
    private IssueCouponUseCase issueCouponUseCase;

    @Autowired
    private CreateOrderFromProductUseCase createOrderFromProductUseCase;

    @Autowired
    private CouponMemoryRepository couponRepository;

    @Autowired
    private UserCouponMemoryRepository userCouponRepository;

    @Autowired
    private ProductMemoryRepository productRepository;

    @Autowired
    private UserMemoryRepository userRepository;

    private Product testProduct;
    private Coupon limitedCoupon;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 생성 (충분한 재고)
        testProduct = Product.createProduct(
                "쿠폰 테스트 상품",  // name
                1L,  // categoryId
                "동시성 테스트용",  // description
                new BigDecimal("50000"),  // price
                1000,  // stock (충분한 재고)
                1,     // minOrderQuantity
                100    // maxOrderQuantity
        );
        testProduct = productRepository.save(testProduct);

        // 총 발급 횟수가 제한된 쿠폰 생성 (10개만 사용 가능)
        limitedCoupon = Coupon.createCoupon(
                "제한 쿠폰",           // name
                "LIMITED10",          // code
                DiscountType.FIXED,   // discountType
                new BigDecimal("5000"),  // discountValue (5000원 할인)
                null,  // maxDiscountAmount
                new BigDecimal("30000"),  // minOrderAmount (최소 주문 금액 30000원)
                10,  // totalQuantity (총 10개만 사용 가능)
                1,   // perUserLimit (사용자당 1번만 사용 가능)
                LocalDateTime.now(),  // startDate
                LocalDateTime.now().plusDays(30)  // endDate
        );
        limitedCoupon = couponRepository.save(limitedCoupon);
    }

    @Test
    @DisplayName("총 발급 횟수가 제한된 쿠폰을 동시에 발급받을 때 제한 횟수만큼만 성공해야 한다")
    void testConcurrentCouponIssuanceWithTotalLimit() throws InterruptedException {
        // given
        int userCount = 20;  // 20명이 동시에 쿠폰 발급 시도
        int couponLimit = 10;  // 쿠폰은 10명에게만 발급 가능

        // 미리 사용자들 생성
        User[] users = new User[userCount];
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < userCount; i++) {
            users[i] = new User(
                    "coupon_user_" + i,  // username
                    "password",  // password
                    BigDecimal.ZERO,  // pointBalance
                    now,  // createdAt
                    now   // updatedAt
            );
            users[i] = userRepository.save(users[i]);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 20명이 동시에 쿠폰 발급 시도
        for (int i = 0; i < userCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    // 쿠폰 발급 (IssueCouponUseCase 사용 - 동시성 제어)
                    IssueCouponCommand issueCommand = new IssueCouponCommand(
                            users[userIndex].getId(),
                            limitedCoupon.getId()
                    );
                    issueCouponUseCase.execute(issueCommand);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    // 쿠폰 발급 실패 (수량 초과)
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        // 1. 쿠폰 제한 횟수(10)만큼만 발급 성공해야 함
        assertThat(successCount.get()).isEqualTo(couponLimit);

        // 2. 나머지 10명은 발급 실패해야 함
        assertThat(failCount.get()).isEqualTo(userCount - couponLimit);

        // 3. 쿠폰 발급 수량이 정확히 10이어야 함
        Coupon finalCoupon = couponRepository.findById(limitedCoupon.getId()).orElseThrow();
        assertThat(finalCoupon.getIssuedQuantity()).isEqualTo(couponLimit);
    }

    @Test
    @DisplayName("동일 사용자가 쿠폰을 동시에 여러 번 사용 시도할 때 1번만 성공해야 한다")
    void testConcurrentCouponUsageBySameUser() throws InterruptedException {
        // given
        // 사용자당 1번만 사용 가능한 쿠폰 (총 10번 사용 가능)
        Coupon perUserLimitCoupon = Coupon.createCoupon(
                "사용자당 1번 쿠폰",  // name
                "PERUSER1",          // code
                DiscountType.FIXED,  // discountType
                new BigDecimal("3000"),  // discountValue
                null,  // maxDiscountAmount
                new BigDecimal("20000"),  // minOrderAmount
                10,  // totalQuantity (총 10번 사용 가능)
                1,   // perUserLimit (사용자당 1번만)
                LocalDateTime.now(),  // startDate
                LocalDateTime.now().plusDays(30)  // endDate
        );
        perUserLimitCoupon = couponRepository.save(perUserLimitCoupon);
        final Long couponId = perUserLimitCoupon.getId();  // Lambda를 위한 final 변수

        // 테스트 사용자 생성
        LocalDateTime now = LocalDateTime.now();
        User user = new User(
                "same_user_coupon",  // username
                "password",  // password
                BigDecimal.ZERO,  // pointBalance
                now,  // createdAt
                now   // updatedAt
        );
        user = userRepository.save(user);
        final Long userId = user.getId();  // Lambda를 위한 final 변수

        // 사용자에게 쿠폰 발급 (IssueCouponUseCase 사용)
        IssueCouponCommand issueCommand = new IssueCouponCommand(userId, couponId);
        issueCouponUseCase.execute(issueCommand);

        // 동시에 5번 주문 시도 (모두 같은 쿠폰 사용)
        int orderAttempts = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(orderAttempts);
        CountDownLatch latch = new CountDownLatch(orderAttempts);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < orderAttempts; i++) {
            executorService.submit(() -> {
                try {
                    CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                            userId,
                            testProduct.getId(),
                            1,
                            null,
                            couponId
                    );

                    createOrderFromProductUseCase.execute(command);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        // 사용자당 1번만 사용 가능하므로 1번만 성공
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(4);

        // UserCoupon 사용 횟수 확인
        UserCoupon finalUserCoupon = userCouponRepository
                .findByUserIdAndCouponId(userId, couponId)
                .orElseThrow();
        assertThat(finalUserCoupon.getUsedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 사용자가 각자의 쿠폰을 동시에 사용할 때 모두 성공해야 한다")
    void testConcurrentCouponUsageByMultipleUsers() throws InterruptedException {
        // given
        // 충분한 사용 횟수를 가진 쿠폰
        Coupon abundantCoupon = Coupon.createCoupon(
                "넉넉한 쿠폰",        // name
                "ABUNDANT",         // code
                DiscountType.PERCENTAGE,  // discountType
                new BigDecimal("10"),  // discountValue (10% 할인)
                new BigDecimal("10000"),  // maxDiscountAmount (최대 10000원 할인)
                new BigDecimal("10000"),  // minOrderAmount
                100,  // totalQuantity (총 100번 사용 가능)
                3,    // perUserLimit (사용자당 3번)
                LocalDateTime.now(),  // startDate
                LocalDateTime.now().plusDays(30)  // endDate
        );
        abundantCoupon = couponRepository.save(abundantCoupon);
        final Long couponId = abundantCoupon.getId();  // Lambda를 위한 final 변수

        int userCount = 20;

        // 미리 사용자들 생성 및 쿠폰 발급
        User[] users = new User[userCount];
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < userCount; i++) {
            users[i] = new User(
                    "multi_coupon_user_" + i,  // username
                    "password",  // password
                    BigDecimal.ZERO,  // pointBalance
                    now,  // createdAt
                    now   // updatedAt
            );
            users[i] = userRepository.save(users[i]);

            // IssueCouponUseCase를 사용하여 쿠폰 발급
            IssueCouponCommand issueCommand = new IssueCouponCommand(users[i].getId(), couponId);
            issueCouponUseCase.execute(issueCommand);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < userCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    // 미리 생성된 사용자 사용

                    CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                            users[userIndex].getId(),
                            testProduct.getId(),
                            1,
                            null,
                            couponId
                    );

                    createOrderFromProductUseCase.execute(command);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        // 쿠폰이 충분하므로 모두 성공해야 함
        assertThat(successCount.get()).isEqualTo(userCount);
        assertThat(failCount.get()).isEqualTo(0);

        Coupon finalCoupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(finalCoupon.getIssuedQuantity()).isEqualTo(userCount);
    }

    @Test
    @DisplayName("같은 사용자가 동시에 같은 쿠폰을 여러 번 발급 시도할 때 1번만 성공해야 한다")
    void testSameUserConcurrentDuplicateIssuance() throws InterruptedException {
        // given
        // 충분한 수량의 쿠폰 생성
        Coupon sameCoupon = Coupon.createCoupon(
                "중복 방지 테스트 쿠폰",  // name
                "DUPLICATE_TEST",         // code
                DiscountType.FIXED,       // discountType
                new BigDecimal("5000"),   // discountValue
                null,                     // maxDiscountAmount
                new BigDecimal("10000"),  // minOrderAmount
                100,                      // totalQuantity (충분한 수량)
                1,                        // perUserLimit (사용자당 1번만)
                LocalDateTime.now(),      // startDate
                LocalDateTime.now().plusDays(30)  // endDate
        );
        sameCoupon = couponRepository.save(sameCoupon);
        final Long couponId = sameCoupon.getId();

        // 테스트 사용자 1명 생성
        LocalDateTime now = LocalDateTime.now();
        User sameUser = new User(
                "duplicate_test_user",  // username
                "password",  // password
                BigDecimal.ZERO,  // pointBalance
                now,  // createdAt
                now   // updatedAt
        );
        sameUser = userRepository.save(sameUser);
        final Long userId = sameUser.getId();

        // 동시에 5번 발급 시도 (같은 사용자, 같은 쿠폰)
        int attemptCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(attemptCount);
        CountDownLatch latch = new CountDownLatch(attemptCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 같은 사용자가 동시에 5번 발급 시도
        for (int i = 0; i < attemptCount; i++) {
            executorService.submit(() -> {
                try {
                    IssueCouponCommand issueCommand = new IssueCouponCommand(userId, couponId);
                    issueCouponUseCase.execute(issueCommand);
                    successCount.incrementAndGet();
                } catch (CouponException e) {
                    // 중복 발급 예외 (COUPON_ALREADY_ISSUED)
                    if (e.getErrorCode() == ErrorCode.COUPON_ALREADY_ISSUED) {
                        failCount.incrementAndGet();
                    } else {
                        // 예상치 못한 예외
                        System.err.println("Unexpected exception: " + e.getMessage());
                    }
                } catch (Exception e) {
                    System.err.println("Unexpected exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        // 같은 사용자는 1번만 발급받아야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(4);

        // UserCoupon도 1개만 생성되어야 함
        UserCoupon userCoupon = userCouponRepository
                .findByUserIdAndCouponId(userId, couponId)
                .orElseThrow();
        assertThat(userCoupon).isNotNull();

        // 쿠폰 발급 수량도 1개만 증가해야 함
        Coupon finalCoupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(finalCoupon.getIssuedQuantity()).isEqualTo(1);
    }
}