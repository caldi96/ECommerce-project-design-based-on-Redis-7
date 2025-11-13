package io.hhplus.ECommerce.ECommerce_project.integration.concurrency;

import io.hhplus.ECommerce.ECommerce_project.order.application.CreateOrderFromProductUseCase;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderResponse;
import io.hhplus.ECommerce.ECommerce_project.payment.application.CreatePaymentUseCase;
import io.hhplus.ECommerce.ECommerce_project.payment.application.command.CreatePaymentCommand;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentMethod;
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
 * 결제 동시성 통합 테스트
 *
 * 시나리오:
 * - 동일한 주문에 대해 여러 번 결제 시도하는 경우
 * - 주문 상태가 올바르게 관리되는지 검증
 */
@SpringBootTest
public class PaymentConcurrencyTest {

    @Autowired
    private CreatePaymentUseCase createPaymentUseCase;

    @Autowired
    private CreateOrderFromProductUseCase createOrderFromProductUseCase;

    @Autowired
    private OrderMemoryRepository orderRepository;

    @Autowired
    private ProductMemoryRepository productRepository;

    @Autowired
    private UserMemoryRepository userRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        LocalDateTime now = LocalDateTime.now();
        testUser = new User(
                "payment_test_user",  // username
                "password",  // password
                BigDecimal.ZERO,  // pointBalance
                now,  // createdAt
                now   // updatedAt
        );
        testUser = userRepository.save(testUser);

        // 테스트용 상품 생성
        testProduct = Product.createProduct(
                "결제 테스트 상품",  // name
                1L,  // categoryId
                "동시 결제 테스트용",  // description
                new BigDecimal("10000"),  // price
                100,  // stock (충분한 재고)
                1,    // minOrderQuantity
                10    // maxOrderQuantity
        );
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("동일 주문에 대해 동시 결제 시도 시 1번만 성공해야 한다")
    void testConcurrentPaymentForSameOrder() throws InterruptedException {
        // given
        // 먼저 주문 생성 (PENDING 상태)
        CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                1,
                null,
                null
        );
        CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);
        Long orderId = orderResponse.orderId();

        // 주문 상태 확인
        Orders order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

        // 동시에 5번 결제 시도
        int paymentAttempts = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(paymentAttempts);
        CountDownLatch latch = new CountDownLatch(paymentAttempts);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < paymentAttempts; i++) {
            executorService.submit(() -> {
                try {
                    CreatePaymentCommand paymentCommand = CreatePaymentCommand.of(
                            orderId,
                            PaymentMethod.CARD
                    );

                    createPaymentUseCase.execute(paymentCommand);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    // 이미 결제된 주문이므로 실패
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        // 1. 1번만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);

        // 2. 나머지 4번은 실패해야 함
        assertThat(failCount.get()).isEqualTo(4);

        // 3. 주문 상태는 PAID여야 함
        Orders finalOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("여러 주문에 대해 동시 결제 시 모두 성공해야 한다")
    void testConcurrentPaymentsForDifferentOrders() throws InterruptedException {
        // given
        int orderCount = 10;
        Long[] orderIds = new Long[orderCount];

        // 10개의 주문 생성
        for (int i = 0; i < orderCount; i++) {
            LocalDateTime now = LocalDateTime.now();
            User user = new User(
                    "multi_payment_user_" + i,  // username
                    "password",  // password
                    BigDecimal.ZERO,  // pointBalance
                    now,  // createdAt
                    now   // updatedAt
            );
            user = userRepository.save(user);

            CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                    user.getId(),
                    testProduct.getId(),
                    1,
                    null,
                    null
            );
            CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);
            orderIds[i] = orderResponse.orderId();
        }

        // 동시에 결제
        ExecutorService executorService = Executors.newFixedThreadPool(orderCount);
        CountDownLatch latch = new CountDownLatch(orderCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < orderCount; i++) {
            final Long orderId = orderIds[i];
            executorService.submit(() -> {
                try {
                    CreatePaymentCommand paymentCommand = CreatePaymentCommand.of(
                            orderId,
                            PaymentMethod.CARD
                    );

                    createPaymentUseCase.execute(paymentCommand);
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
        // 모든 결제가 성공해야 함
        assertThat(successCount.get()).isEqualTo(orderCount);
        assertThat(failCount.get()).isEqualTo(0);

        // 모든 주문이 PAID 상태여야 함
        for (Long orderId : orderIds) {
            Orders order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }
    }

    @Test
    @DisplayName("주문과 결제가 동시에 발생할 때 순서가 올바르게 처리되어야 한다")
    void testConcurrentOrderAndPayment() throws InterruptedException {
        // given
        int count = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(count * 2);
        CountDownLatch orderLatch = new CountDownLatch(count);
        CountDownLatch paymentLatch = new CountDownLatch(count);

        Long[] orderIds = new Long[count];
        AtomicInteger orderSuccessCount = new AtomicInteger(0);
        AtomicInteger paymentSuccessCount = new AtomicInteger(0);

        // when - 주문 생성
        for (int i = 0; i < count; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    LocalDateTime now = LocalDateTime.now();
                    User user = new User(
                            "concurrent_user_" + index,  // username
                            "password",  // password
                            BigDecimal.ZERO,  // pointBalance
                            now,  // createdAt
                            now   // updatedAt
                    );
                    user = userRepository.save(user);

                    CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                            user.getId(),
                            testProduct.getId(),
                            1,
                            null,
                            null
                    );
                    CreateOrderResponse response = createOrderFromProductUseCase.execute(orderCommand);
                    orderIds[index] = response.orderId();
                    orderSuccessCount.incrementAndGet();

                } catch (Exception e) {
                    // 주문 실패
                } finally {
                    orderLatch.countDown();
                }
            });
        }

        // 주문 완료 대기
        orderLatch.await();

        // when - 결제 시도
        for (int i = 0; i < count; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    if (orderIds[index] != null) {
                        CreatePaymentCommand paymentCommand = CreatePaymentCommand.of(
                                orderIds[index],
                                PaymentMethod.CARD
                        );
                        createPaymentUseCase.execute(paymentCommand);
                        paymentSuccessCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 결제 실패
                } finally {
                    paymentLatch.countDown();
                }
            });
        }

        paymentLatch.await();
        executorService.shutdown();

        // then
        assertThat(orderSuccessCount.get()).isEqualTo(count);
        assertThat(paymentSuccessCount.get()).isEqualTo(count);

        // 모든 주문이 PAID 상태여야 함
        for (Long orderId : orderIds) {
            if (orderId != null) {
                Orders order = orderRepository.findById(orderId).orElseThrow();
                assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            }
        }
    }

    @Test
    @DisplayName("PENDING이 아닌 주문은 결제할 수 없어야 한다")
    void testPaymentOnlyForCompletedOrder() throws InterruptedException {
        // given
        // 주문 생성
        CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                1,
                null,
                null
        );
        CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);
        Long orderId = orderResponse.orderId();

        // 먼저 한 번 결제 (PAID 상태로 만들기)
        CreatePaymentCommand firstPayment = CreatePaymentCommand.of(orderId, PaymentMethod.CARD);
        createPaymentUseCase.execute(firstPayment);

        // 주문 상태 확인
        Orders order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        // when - PAID 상태인 주문에 대해 다시 결제 시도
        int paymentAttempts = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(paymentAttempts);
        CountDownLatch latch = new CountDownLatch(paymentAttempts);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < paymentAttempts; i++) {
            executorService.submit(() -> {
                try {
                    CreatePaymentCommand paymentCommand = CreatePaymentCommand.of(
                            orderId,
                            PaymentMethod.CARD
                    );
                    createPaymentUseCase.execute(paymentCommand);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    // PAID 상태에서는 결제 불가
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        // 모든 시도가 실패해야 함
        assertThat(successCount.get()).isEqualTo(0);
        assertThat(failCount.get()).isEqualTo(paymentAttempts);

        // 주문 상태는 여전히 PAID
        Orders finalOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }
}