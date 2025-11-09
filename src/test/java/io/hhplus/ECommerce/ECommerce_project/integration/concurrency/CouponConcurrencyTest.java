package io.hhplus.ECommerce.ECommerce_project.integration.concurrency;

import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.CreateOrderFromProductUseCase;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserRepository;
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
 * 쿠폰 동시성 통합 테스트
 *
 * 시나리오:
 * - 제한된 쿠폰을 여러 사용자가 동시에 사용하는 경우
 * - 사용자별 쿠폰 사용 횟수 제한이 있는 경우
 */
@SpringBootTest
public class CouponConcurrencyTest {

    @Autowired
    private CreateOrderFromProductUseCase createOrderFromProductUseCase;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

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
    @DisplayName("총 발급 횟수가 제한된 쿠폰을 동시에 사용할 때 제한 횟수만큼만 성공해야 한다")
    void testConcurrentCouponUsageWithTotalLimit() throws InterruptedException {
        // given
        int userCount = 20;  // 20명이 동시에 쿠폰 사용 시도
        int couponLimit = 10;  // 쿠폰은 10개만 사용 가능

        // 미리 사용자들 생성 및 쿠폰 발급
        User[] users = new User[userCount];
        UserCoupon[] userCoupons = new UserCoupon[userCount];
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < userCount; i++) {
            users[i] = new User(
                    null,  // id
                    "coupon_user_" + i,  // username
                    "password",  // password
                    BigDecimal.ZERO,  // pointBalance
                    now,  // createdAt
                    now   // updatedAt
            );
            users[i] = userRepository.save(users[i]);

            // 사용자에게 쿠폰 발급
            userCoupons[i] = UserCoupon.issueCoupon(users[i].getId(), limitedCoupon.getId());
            userCouponRepository.save(userCoupons[i]);
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

                    // 쿠폰을 사용하여 주문
                    CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                            users[userIndex].getId(),
                            testProduct.getId(),
                            1,  // 1개씩 주문
                            null,  // 포인트 사용 없음
                            limitedCoupon.getId()  // 쿠폰 사용
                    );

                    createOrderFromProductUseCase.execute(command);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    // 쿠폰 사용 횟수 초과로 실패
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        // 1. 쿠폰 제한 횟수(10)만큼만 성공해야 함
        assertThat(successCount.get()).isEqualTo(couponLimit);

        // 2. 나머지는 실패해야 함
        assertThat(failCount.get()).isEqualTo(userCount - couponLimit);

        // 3. 쿠폰 사용 횟수가 10이어야 함
        Coupon finalCoupon = couponRepository.findById(limitedCoupon.getId()).orElseThrow();
        assertThat(finalCoupon.getUsageCount()).isEqualTo(couponLimit);
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
                null,  // id
                "same_user_coupon",  // username
                "password",  // password
                BigDecimal.ZERO,  // pointBalance
                now,  // createdAt
                now   // updatedAt
        );
        user = userRepository.save(user);
        final Long userId = user.getId();  // Lambda를 위한 final 변수

        // 사용자에게 쿠폰 발급
        UserCoupon userCoupon = UserCoupon.issueCoupon(userId, couponId);
        userCouponRepository.save(userCoupon);

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
                    null,  // id
                    "multi_coupon_user_" + i,  // username
                    "password",  // password
                    BigDecimal.ZERO,  // pointBalance
                    now,  // createdAt
                    now   // updatedAt
            );
            users[i] = userRepository.save(users[i]);

            UserCoupon userCoupon = UserCoupon.issueCoupon(users[i].getId(), couponId);
            userCouponRepository.save(userCoupon);
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
        assertThat(finalCoupon.getUsageCount()).isEqualTo(userCount);
    }
}