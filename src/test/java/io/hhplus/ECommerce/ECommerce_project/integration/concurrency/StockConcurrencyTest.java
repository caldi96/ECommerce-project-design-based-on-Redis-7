package io.hhplus.ECommerce.ECommerce_project.integration.concurrency;

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
 * 재고 동시성 통합 테스트
 *
 * 시나리오:
 * - 재고가 10개인 상품에 대해 20명의 사용자가 동시에 1개씩 주문
 * - 비관적 락을 통해 10개만 성공하고 10개는 실패해야 함
 */
@SpringBootTest
public class StockConcurrencyTest {

    @Autowired
    private CreateOrderFromProductUseCase createOrderFromProductUseCase;

    @Autowired
    private ProductMemoryRepository productRepository;

    @Autowired
    private UserMemoryRepository userRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 생성 (재고 10개)
        testProduct = Product.createProduct(
                "동시성 테스트 상품",  // name
                1L,  // categoryId
                "재고 10개 테스트용",  // description
                new BigDecimal("10000"),  // price
                10,  // stock (재고 10개)
                1,   // minOrderQuantity
                10   // maxOrderQuantity
        );
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("동시에 20명이 주문할 때 재고 10개만 차감되고 10명만 성공해야 한다")
    void testConcurrentStockDecrease() throws InterruptedException {
        // given
        int totalThreads = 20;  // 동시 요청 수
        int initialStock = 10;  // 초기 재고

        // 미리 사용자들 생성
        User[] users = new User[totalThreads];
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < totalThreads; i++) {
            users[i] = new User(
                    "concurrency_user_" + i,  // username
                    "password",  // password
                    BigDecimal.ZERO,  // pointBalance
                    now,  // createdAt
                    now   // updatedAt
            );
            users[i] = userRepository.save(users[i]);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch latch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < totalThreads; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    // 미리 생성된 사용자 사용

                    // 주문 시도
                    CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                            users[userIndex].getId(),
                            testProduct.getId(),
                            1,  // 1개씩 주문
                            null,  // 포인트 사용 없음
                            null   // 쿠폰 없음
                    );

                    createOrderFromProductUseCase.execute(command);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    // 재고 부족으로 실패한 경우
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await();
        executorService.shutdown();

        // then
        // 1. 성공한 주문은 10개여야 함
        assertThat(successCount.get()).isEqualTo(initialStock);

        // 2. 실패한 주문은 10개여야 함
        assertThat(failCount.get()).isEqualTo(totalThreads - initialStock);

        // 3. 최종 재고는 0이어야 함
        Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(finalProduct.getStock()).isEqualTo(0);

        // 4. 판매량은 10이어야 함
        assertThat(finalProduct.getSoldCount()).isEqualTo(initialStock);
    }

    @Test
    @DisplayName("동일 사용자가 동시에 여러 번 주문할 때 재고가 정확히 차감되어야 한다")
    void testConcurrentOrdersBySameUser() throws InterruptedException {
        // given
        int orderCount = 5;  // 동시 주문 횟수
        ExecutorService executorService = Executors.newFixedThreadPool(orderCount);
        CountDownLatch latch = new CountDownLatch(orderCount);

        // 테스트 사용자 생성
        LocalDateTime now = LocalDateTime.now();
        User user = new User(
                "same_user_test",  // username
                "password",  // password
                BigDecimal.ZERO,  // pointBalance
                now,  // createdAt
                now   // updatedAt
        );
        user = userRepository.save(user);
        final Long userId = user.getId();  // Lambda를 위한 final 변수

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < orderCount; i++) {
            executorService.submit(() -> {
                try {
                    CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                            userId,
                            testProduct.getId(),
                            2,  // 2개씩 주문
                            null,
                            null
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
        // 재고 10개에서 2개씩 주문하므로 5번 중 5번 성공해야 함
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failCount.get()).isEqualTo(0);

        Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(finalProduct.getStock()).isEqualTo(0);
        assertThat(finalProduct.getSoldCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("재고가 부족한 상황에서 동시 주문 시 일부만 성공해야 한다")
    void testPartialSuccessWhenInsufficientStock() throws InterruptedException {
        // given
        // 재고 3개인 상품 생성
        Product limitedProduct = Product.createProduct(
                "재고 부족 테스트 상품",  // name
                1L,  // categoryId
                "재고 3개",  // description
                new BigDecimal("5000"),  // price
                3,   // stock
                1,   // minOrderQuantity
                10   // maxOrderQuantity
        );
        limitedProduct = productRepository.save(limitedProduct);
        final Long limitedProductId = limitedProduct.getId();  // Lambda를 위한 final 변수

        int totalOrders = 10;  // 10명이 주문 시도

        // 미리 사용자들 생성
        User[] users = new User[totalOrders];
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < totalOrders; i++) {
            users[i] = new User(
                    "limited_user_" + i,  // username
                    "password",  // password
                    BigDecimal.ZERO,  // pointBalance
                    now,  // createdAt
                    now   // updatedAt
            );
            users[i] = userRepository.save(users[i]);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(totalOrders);
        CountDownLatch latch = new CountDownLatch(totalOrders);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < totalOrders; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    // 미리 생성된 사용자 사용

                    CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                            users[userIndex].getId(),
                            limitedProductId,
                            1,
                            null,
                            null
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
        assertThat(successCount.get()).isEqualTo(3);  // 재고 3개이므로 3명만 성공
        assertThat(failCount.get()).isEqualTo(7);     // 나머지 7명은 실패

        Product finalProduct = productRepository.findById(limitedProduct.getId()).orElseThrow();
        assertThat(finalProduct.getStock()).isEqualTo(0);
        assertThat(finalProduct.getSoldCount()).isEqualTo(3);
    }
}