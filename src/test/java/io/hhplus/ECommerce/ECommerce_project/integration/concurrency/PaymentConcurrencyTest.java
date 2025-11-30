package io.hhplus.ECommerce.ECommerce_project.integration.concurrency;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.CreateOrderFromProductUseCase;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderResponse;
import io.hhplus.ECommerce.ECommerce_project.payment.application.CreatePaymentUseCase;
import io.hhplus.ECommerce.ECommerce_project.payment.application.command.CreatePaymentCommand;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.entity.Payment;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentMethod;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentStatus;
import io.hhplus.ECommerce.ECommerce_project.payment.infrastructure.PaymentRepository;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.RedisStockService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 결제 동시성 통합 테스트
 *
 * 시나리오:
 * - 같은 주문을 동시에 여러 번 결제 시도 (1번만 성공해야 함)
 * - 여러 주문을 동시에 결제 (모두 성공해야 함)
 * - 결제 후 주문 상태 변경 확인
 */
@SpringBootTest
@ActiveProfiles("integration")
public class PaymentConcurrencyTest {

    @Autowired
    private CreatePaymentUseCase createPaymentUseCase;

    @Autowired
    private CreateOrderFromProductUseCase createOrderFromProductUseCase;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RedisStockService redisStockService;

    private Product testProduct;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리 (외래 키 제약조건 고려)
        paymentRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트 카테고리 생성 (고유한 순서 사용, 1-10000)
        testCategory = Category.createCategory("결제테스트카테고리", (int) (System.currentTimeMillis() % 10000) + 1);
        testCategory = categoryRepository.save(testCategory);

        // 테스트용 상품 생성
        testProduct = Product.createProduct(
                testCategory,
                "결제 테스트 상품",
                "동시성 테스트용",
                BigDecimal.valueOf(10000),
                1000,
                1,
                10
        );
        testProduct = productRepository.save(testProduct);

        // 레디스 재고 동기화 (중요!)
        redisStockService.setStock(testProduct.getId(), testProduct.getStock());
    }

    @Test
    @DisplayName("같은 주문을 동시에 여러 번 결제 시도할 때 1번만 성공해야 한다")
    void testConcurrentPaymentForSameOrder() throws InterruptedException {
        // Given
        User user = new User();
        user.setUsername("payment_user");
        user.setPassword("password");
        user.setPointBalance(BigDecimal.ZERO);
        user = userRepository.save(user);

        // 주문 생성
        CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                user.getId(),
                testProduct.getId(),
                1,
                null,
                null
        );
        CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);
        final Long orderId = orderResponse.orderId();

        int attemptCount = 5;
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
                    CreatePaymentCommand paymentCommand = new CreatePaymentCommand(
                            orderId,
                            PaymentMethod.CARD
                    );
                    createPaymentUseCase.execute(paymentCommand);
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
        assertThat(failCount.get()).isEqualTo(4);

        // Payment가 1개만 생성되었는지 확인
        List<Payment> payments = paymentRepository.findByOrder_Id(orderId);
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);

        // 주문 상태가 PAID로 변경되었는지 확인
        Orders order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("여러 주문을 동시에 결제할 때 모두 성공해야 한다")
    void testConcurrentPaymentForMultipleOrders() throws InterruptedException {
        // Given
        int orderCount = 10;

        // 사용자들 생성 및 주문 생성
        User[] users = new User[orderCount];
        Long[] orderIds = new Long[orderCount];

        for (int i = 0; i < orderCount; i++) {
            users[i] = new User();
            users[i].setUsername("multi_payment_user_" + i);
            users[i].setPassword("password");
            users[i].setPointBalance(BigDecimal.ZERO);
            users[i] = userRepository.save(users[i]);

            CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                    users[i].getId(),
                    testProduct.getId(),
                    1,
                    null,
                    null
            );
            CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);
            orderIds[i] = orderResponse.orderId();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(orderCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(orderCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < orderCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    CreatePaymentCommand paymentCommand = new CreatePaymentCommand(
                            orderIds[index],
                            PaymentMethod.CARD
                    );
                    createPaymentUseCase.execute(paymentCommand);
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
        assertThat(successCount.get()).isEqualTo(orderCount);
        assertThat(failCount.get()).isEqualTo(0);

        // 모든 주문이 PAID 상태인지 확인
        for (Long orderId : orderIds) {
            Orders order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

            // 각 주문마다 Payment가 1개씩 생성되었는지 확인
            List<Payment> payments = paymentRepository.findByOrder_Id(orderId);
            assertThat(payments).hasSize(1);
            assertThat(payments.get(0).getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }
    }

    @Test
    @DisplayName("동일 사용자가 여러 주문을 동시에 결제할 때 모두 성공해야 한다")
    void testConcurrentPaymentBySameUser() throws InterruptedException {
        // Given
        User user = new User();
        user.setUsername("same_user_payment");
        user.setPassword("password");
        user.setPointBalance(BigDecimal.ZERO);
        user = userRepository.save(user);
        final Long userId = user.getId();

        int orderCount = 5;
        Long[] orderIds = new Long[orderCount];

        // 같은 사용자의 주문 생성
        for (int i = 0; i < orderCount; i++) {
            CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                    userId,
                    testProduct.getId(),
                    1,
                    null,
                    null
            );
            CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);
            orderIds[i] = orderResponse.orderId();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(orderCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(orderCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < orderCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    CreatePaymentCommand paymentCommand = new CreatePaymentCommand(
                            orderIds[index],
                            PaymentMethod.CARD
                    );
                    createPaymentUseCase.execute(paymentCommand);
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
        assertThat(successCount.get()).isEqualTo(orderCount);
        assertThat(failCount.get()).isEqualTo(0);

        // 모든 주문이 PAID 상태인지 확인
        for (Long orderId : orderIds) {
            Orders order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

            // 각 주문마다 Payment가 1개씩 생성되었는지 확인
            List<Payment> payments = paymentRepository.findByOrder_Id(orderId);
            assertThat(payments).hasSize(1);
        }
    }
}
