package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderResponse;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.infrastructure.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.point.infrastructure.PointUsageHistoryRepository;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.infrastructure.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("integration")
class CreateOrderFromProductUseCaseIntegrationTest {

    @Autowired
    private CreateOrderFromProductUseCase createOrderFromProductUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private PointUsageHistoryRepository pointUsageHistoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private User testUser;
    private Category testCategory;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성 (포인트는 0으로 시작)
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setPointBalance(BigDecimal.ZERO);
        testUser = userRepository.save(testUser);

        // 테스트 카테고리 생성
        testCategory = Category.createCategory("테스트카테고리", 1);
        testCategory = categoryRepository.save(testCategory);

        // 테스트 상품 생성
        testProduct = Product.createProduct(
                testCategory,
                "테스트상품",
                "상품 설명",
                BigDecimal.valueOf(10000),
                100,
                1,
                10
        );
        testProduct = productRepository.save(testProduct);
    }

    @AfterEach
    void tearDown() {
        // 외래 키 제약조건을 고려한 순서로 삭제
        pointUsageHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        pointRepository.deleteAll();
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("정상적으로 상품에서 주문이 생성된다 (쿠폰X, 포인트X)")
    void createOrder_success_withoutCouponAndPoint() {
        // Given
        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                3,
                null,  // 포인트 사용 안함
                null   // 쿠폰 사용 안함
        );

        int initialStock = testProduct.getStock();

        // When
        CreateOrderResponse response = createOrderFromProductUseCase.execute(command);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isNotNull();
        assertThat(response.orderItems()).hasSize(1);

        // 주문 확인
        Orders order = orderRepository.findById(response.orderId()).orElseThrow();
        assertThat(order.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(30000)); // 10000 * 3

        // 재고 감소 확인
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(initialStock - 3);

        // 판매량 증가 확인
        assertThat(updatedProduct.getSoldCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("쿠폰을 사용하여 주문이 생성된다")
    void createOrder_success_withCoupon() {
        // Given
        // 20% 할인 쿠폰 생성
        Coupon coupon = Coupon.createCoupon(
                "20% 할인 쿠폰",
                "COUPON20",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(10000),  // 최대 할인 금액
                BigDecimal.valueOf(20000),  // 최소 주문 금액
                100,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        coupon = couponRepository.save(coupon);

        // 사용자에게 쿠폰 발급
        UserCoupon userCoupon = UserCoupon.issueCoupon(testUser, coupon);
        userCoupon = userCouponRepository.save(userCoupon);

        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                3,  // 30000원 (최소 주문 금액 충족)
                null,  // 포인트 사용 안함
                coupon.getId()
        );

        // When
        CreateOrderResponse response = createOrderFromProductUseCase.execute(command);

        // Then
        assertThat(response).isNotNull();
        Orders order = orderRepository.findById(response.orderId()).orElseThrow();

        // 할인 금액 확인 (30000 * 20% = 6000)
        assertThat(order.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(6000));

        // 쿠폰 사용 횟수 확인
        UserCoupon updatedUserCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(updatedUserCoupon.getUsedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("포인트를 사용하여 주문이 생성된다")
    void createOrder_success_withPoint() {
        // Given
        // 사용자에게 포인트 충전
        Point point = Point.charge(testUser, BigDecimal.valueOf(5000), "테스트 충전");
        point = pointRepository.save(point);

        // User의 pointBalance도 동기화
        testUser.chargePoint(BigDecimal.valueOf(5000));
        testUser = userRepository.save(testUser);

        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                2,
                BigDecimal.valueOf(3000),  // 포인트 3000원 사용
                null   // 쿠폰 사용 안함
        );

        // When
        CreateOrderResponse response = createOrderFromProductUseCase.execute(command);

        // Then
        assertThat(response).isNotNull();
        Orders order = orderRepository.findById(response.orderId()).orElseThrow();

        // 포인트 사용 금액 확인
        assertThat(order.getPointAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));

        // 사용자 포인트 잔액 확인 (5000 충전 - 3000 사용 = 2000)
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getPointBalance()).isEqualByComparingTo(BigDecimal.valueOf(2000));

        // 포인트 사용 확인
        Point updatedPoint = pointRepository.findById(point.getId()).orElseThrow();
        assertThat(updatedPoint.getUsedAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
    }

    @Test
    @DisplayName("쿠폰과 포인트를 모두 사용하여 주문이 생성된다")
    void createOrder_success_withCouponAndPoint() {
        // Given
        // 5000원 할인 쿠폰 생성
        Coupon coupon = Coupon.createCoupon(
                "5000원 할인 쿠폰",
                "COUPON5000",
                DiscountType.FIXED,
                BigDecimal.valueOf(5000),
                null,
                BigDecimal.valueOf(20000),
                100,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        coupon = couponRepository.save(coupon);

        UserCoupon userCoupon = UserCoupon.issueCoupon(testUser, coupon);
        userCoupon = userCouponRepository.save(userCoupon);

        // 포인트 충전
        Point point = Point.charge(testUser, BigDecimal.valueOf(10000), "테스트 충전");
        point = pointRepository.save(point);

        testUser.chargePoint(BigDecimal.valueOf(10000));
        userRepository.save(testUser);

        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                3,  // 30000원
                BigDecimal.valueOf(2000),  // 포인트 2000원 사용
                coupon.getId()
        );

        // When
        CreateOrderResponse response = createOrderFromProductUseCase.execute(command);

        // Then
        assertThat(response).isNotNull();
        Orders order = orderRepository.findById(response.orderId()).orElseThrow();

        // 총 주문 금액: 30000 (상품 총액)
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        // 할인: 5000 (쿠폰)
        assertThat(order.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        // 포인트: 2000
        assertThat(order.getPointAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    @Test
    @DisplayName("재고가 부족하면 주문 생성이 실패한다")
    void createOrder_fail_outOfStock() {
        // Given
        // 재고를 5개로 줄임
        testProduct.updateStock(5);
        productRepository.save(testProduct);

        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                10,  // 재고(5) 보다 많은 수량 주문
                null,
                null
        );

        // When & Then
        assertThrows(ProductException.class, () -> createOrderFromProductUseCase.execute(command));

        // 재고가 변경되지 않았는지 확인
        Product unchangedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(unchangedProduct.getStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("포인트가 부족하면 주문 생성이 실패한다")
    void createOrder_fail_insufficientPoint() {
        // Given
        // 포인트 충전 (1000원만)
        Point point = Point.charge(testUser, BigDecimal.valueOf(1000), "테스트 충전");
        point = pointRepository.save(point);

        testUser.chargePoint(BigDecimal.valueOf(1000));
        userRepository.save(testUser);

        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                2,
                BigDecimal.valueOf(5000),  // 5000원 사용 시도 (잔액 부족)
                null
        );

        // When & Then
        assertThrows(PointException.class, () -> createOrderFromProductUseCase.execute(command));
    }

    @Test
    @DisplayName("비활성화된 상품은 주문할 수 없다")
    void createOrder_fail_inactiveProduct() {
        // Given
        testProduct.deactivate();
        productRepository.save(testProduct);

        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                2,
                null,
                null
        );

        // When & Then
        assertThrows(ProductException.class, () -> createOrderFromProductUseCase.execute(command));
    }

    @Test
    @DisplayName("최소 주문 수량을 충족하지 못하면 주문이 실패한다")
    void createOrder_fail_belowMinOrderQuantity() {
        // Given
        // 최소 주문 수량을 5개로 설정
        testProduct.updateMinOrderQuantity(5);
        productRepository.save(testProduct);

        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                2,  // 최소 주문 수량(5) 미달
                null,
                null
        );

        // When & Then
        assertThrows(ProductException.class, () -> createOrderFromProductUseCase.execute(command));
    }

    @Test
    @DisplayName("최대 주문 수량을 초과하면 주문이 실패한다")
    void createOrder_fail_exceedMaxOrderQuantity() {
        // Given
        // 최대 주문 수량을 5개로 설정
        testProduct.updateMaxOrderQuantity(5);
        productRepository.save(testProduct);

        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                10,  // 최대 주문 수량(5) 초과
                null,
                null
        );

        // When & Then
        assertThrows(ProductException.class, () -> createOrderFromProductUseCase.execute(command));
    }

    @Test
    @DisplayName("여러 개의 포인트를 순차적으로 사용하여 주문한다 (선입선출)")
    void createOrder_success_multiplePointsFIFO() {
        // Given
        // 첫 번째 포인트 충전 (3000원)
        Point point1 = Point.charge(testUser, BigDecimal.valueOf(3000), "첫 번째 충전");
        point1 = pointRepository.save(point1);

        // 두 번째 포인트 충전 (2000원)
        Point point2 = Point.charge(testUser, BigDecimal.valueOf(2000), "두 번째 충전");
        point2 = pointRepository.save(point2);

        testUser.chargePoint(BigDecimal.valueOf(5000));
        userRepository.save(testUser);

        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                2,
                BigDecimal.valueOf(4000),  // 포인트 4000원 사용
                null
        );

        // When
        CreateOrderResponse response = createOrderFromProductUseCase.execute(command);

        // Then
        assertThat(response).isNotNull();

        // 첫 번째 포인트는 전액 사용 (3000원)
        Point updatedPoint1 = pointRepository.findById(point1.getId()).orElseThrow();
        assertThat(updatedPoint1.getUsedAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(updatedPoint1.isUsed()).isTrue();

        // 두 번째 포인트는 일부 사용 (1000원)
        Point updatedPoint2 = pointRepository.findById(point2.getId()).orElseThrow();
        assertThat(updatedPoint2.getUsedAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(updatedPoint2.isUsed()).isFalse();
        assertThat(updatedPoint2.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }
}