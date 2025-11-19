package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.infrastructure.CartRepository;
import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CartException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromCartCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderResponse;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.infrastructure.PointRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class CreateOrderFromCartUseCaseIntegrationTest {

    @Autowired
    private CreateOrderFromCartUseCase createOrderFromCartUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private User testUser;
    private Category testCategory;
    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setPointBalance(BigDecimal.ZERO);
        testUser = userRepository.save(testUser);

        // 테스트 카테고리 생성
        testCategory = Category.createCategory("테스트카테고리", 1);
        testCategory = categoryRepository.save(testCategory);

        // 테스트 상품1 생성
        testProduct1 = Product.createProduct(
                testCategory,
                "테스트상품1",
                "설명1",
                BigDecimal.valueOf(10000),
                100,
                1,
                10
        );
        testProduct1 = productRepository.save(testProduct1);

        // 테스트 상품2 생성
        testProduct2 = Product.createProduct(
                testCategory,
                "테스트상품2",
                "설명2",
                BigDecimal.valueOf(20000),
                50,
                1,
                5
        );
        testProduct2 = productRepository.save(testProduct2);
    }

    @Test
    @DisplayName("정상적으로 장바구니에서 주문이 생성된다 (쿠폰X, 포인트X)")
    void createOrder_success_withoutCouponAndPoint() {
        // Given
        Cart cart1 = Cart.createCart(testUser, testProduct1, 2);
        cart1 = cartRepository.save(cart1);

        Cart cart2 = Cart.createCart(testUser, testProduct2, 1);
        cart2 = cartRepository.save(cart2);

        CreateOrderFromCartCommand command = new CreateOrderFromCartCommand(
                testUser.getId(),
                List.of(cart1.getId(), cart2.getId()),
                null,  // 포인트 사용 안함
                null   // 쿠폰 사용 안함
        );

        int initialStock1 = testProduct1.getStock();
        int initialStock2 = testProduct2.getStock();

        // When
        CreateOrderResponse response = createOrderFromCartUseCase.execute(command);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isNotNull();
        assertThat(response.orderItems()).hasSize(2);

        // 주문 확인
        Orders order = orderRepository.findById(response.orderId()).orElseThrow();
        assertThat(order.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(order.getTotalAmount()).isEqualTo(BigDecimal.valueOf(40000)); // 상품 총액 (상품1: 20000 + 상품2: 20000)

        // 재고 감소 확인
        Product updatedProduct1 = productRepository.findById(testProduct1.getId()).orElseThrow();
        Product updatedProduct2 = productRepository.findById(testProduct2.getId()).orElseThrow();
        assertThat(updatedProduct1.getStock()).isEqualTo(initialStock1 - 2);
        assertThat(updatedProduct2.getStock()).isEqualTo(initialStock2 - 1);

        // 판매량 증가 확인
        assertThat(updatedProduct1.getSoldCount()).isEqualTo(2);
        assertThat(updatedProduct2.getSoldCount()).isEqualTo(1);

        // 장바구니 삭제 확인
        assertThat(cartRepository.findById(cart1.getId())).isEmpty();
        assertThat(cartRepository.findById(cart2.getId())).isEmpty();
    }

    @Test
    @DisplayName("쿠폰을 사용하여 주문이 생성된다")
    void createOrder_success_withCoupon() {
        // Given
        Cart cart = Cart.createCart(testUser, testProduct1, 2);
        cart = cartRepository.save(cart);

        // 10% 할인 쿠폰 생성
        Coupon coupon = Coupon.createCoupon(
                "10% 할인 쿠폰",
                "COUPON10",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(5000),  // 최대 할인 금액
                BigDecimal.valueOf(10000), // 최소 주문 금액
                100,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        coupon = couponRepository.save(coupon);

        // 사용자에게 쿠폰 발급
        UserCoupon userCoupon = UserCoupon.issueCoupon(testUser, coupon);
        userCoupon = userCouponRepository.save(userCoupon);

        CreateOrderFromCartCommand command = new CreateOrderFromCartCommand(
                testUser.getId(),
                List.of(cart.getId()),
                null,  // 포인트 사용 안함
                coupon.getId()
        );

        // When
        CreateOrderResponse response = createOrderFromCartUseCase.execute(command);

        // Then
        assertThat(response).isNotNull();
        Orders order = orderRepository.findById(response.orderId()).orElseThrow();

        // 할인 금액 확인 (20000 * 10% = 2000)
        assertThat(order.getDiscountAmount()).isEqualTo(BigDecimal.valueOf(2000));

        // 쿠폰 사용 횟수 확인
        UserCoupon updatedUserCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(updatedUserCoupon.getUsedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("포인트를 사용하여 주문이 생성된다")
    void createOrder_success_withPoint() {
        // Given
        Cart cart = Cart.createCart(testUser, testProduct1, 2);
        cart = cartRepository.save(cart);

        // 사용자에게 포인트 충전
        Point point = Point.charge(testUser, BigDecimal.valueOf(5000), "테스트 충전");
        point = pointRepository.save(point);

        // User의 pointBalance도 동기화
        testUser.chargePoint(BigDecimal.valueOf(5000));
        testUser = userRepository.save(testUser);

        CreateOrderFromCartCommand command = new CreateOrderFromCartCommand(
                testUser.getId(),
                List.of(cart.getId()),
                BigDecimal.valueOf(3000),  // 포인트 3000원 사용
                null   // 쿠폰 사용 안함
        );

        // When
        CreateOrderResponse response = createOrderFromCartUseCase.execute(command);

        // Then
        assertThat(response).isNotNull();
        Orders order = orderRepository.findById(response.orderId()).orElseThrow();

        // 포인트 사용 금액 확인
        assertThat(order.getPointAmount()).isEqualTo(BigDecimal.valueOf(3000));

        // 사용자 포인트 잔액 확인 (5000 충전 - 3000 사용 = 2000)
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getPointBalance()).isEqualTo(BigDecimal.valueOf(2000));

        // 포인트 사용 확인
        Point updatedPoint = pointRepository.findById(point.getId()).orElseThrow();
        assertThat(updatedPoint.getUsedAmount()).isEqualTo(BigDecimal.valueOf(3000));
    }

    @Test
    @DisplayName("쿠폰과 포인트를 모두 사용하여 주문이 생성된다")
    void createOrder_success_withCouponAndPoint() {
        // Given
        Cart cart = Cart.createCart(testUser, testProduct1, 3);
        cart = cartRepository.save(cart);

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

        CreateOrderFromCartCommand command = new CreateOrderFromCartCommand(
                testUser.getId(),
                List.of(cart.getId()),
                BigDecimal.valueOf(2000),  // 포인트 2000원 사용
                coupon.getId()
        );

        // When
        CreateOrderResponse response = createOrderFromCartUseCase.execute(command);

        // Then
        assertThat(response).isNotNull();
        Orders order = orderRepository.findById(response.orderId()).orElseThrow();

        // 총 주문 금액: 30000 (상품 총액)
        assertThat(order.getTotalAmount()).isEqualTo(BigDecimal.valueOf(30000));
        // 할인: 5000 (쿠폰)
        assertThat(order.getDiscountAmount()).isEqualTo(BigDecimal.valueOf(5000));
        // 포인트: 2000
        assertThat(order.getPointAmount()).isEqualTo(BigDecimal.valueOf(2000));
    }

    @Test
    @DisplayName("재고가 부족하면 주문 생성이 실패한다")
    void createOrder_fail_outOfStock() {
        // Given
        Cart cart = Cart.createCart(testUser, testProduct1, 2);
        cart = cartRepository.save(cart);

        // 재고를 1개로 줄임
        testProduct1.updateStock(1);
        productRepository.save(testProduct1);

        CreateOrderFromCartCommand command = new CreateOrderFromCartCommand(
                testUser.getId(),
                List.of(cart.getId()),
                null,
                null
        );

        // When & Then
        assertThrows(ProductException.class, () -> createOrderFromCartUseCase.execute(command));

        // 재고가 변경되지 않았는지 확인 (롤백)
        Product unchangedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(unchangedProduct.getStock()).isEqualTo(1);
    }

    @Test
    @DisplayName("포인트가 부족하면 주문 생성이 실패한다")
    void createOrder_fail_insufficientPoint() {
        // Given
        Cart cart = Cart.createCart(testUser, testProduct1, 2);
        cart = cartRepository.save(cart);

        // 포인트 충전 (1000원만)
        Point point = Point.charge(testUser, BigDecimal.valueOf(1000), "테스트 충전");
        point = pointRepository.save(point);

        testUser.chargePoint(BigDecimal.valueOf(1000));
        userRepository.save(testUser);

        CreateOrderFromCartCommand command = new CreateOrderFromCartCommand(
                testUser.getId(),
                List.of(cart.getId()),
                BigDecimal.valueOf(5000),  // 5000원 사용 시도 (잔액 부족)
                null
        );

        // When & Then
        assertThrows(PointException.class, () -> createOrderFromCartUseCase.execute(command));
    }

    @Test
    @DisplayName("다른 사용자의 장바구니로 주문 생성 시 실패한다")
    void createOrder_fail_otherUserCart() {
        // Given
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setPassword("password456");
        otherUser.setPointBalance(BigDecimal.ZERO);
        otherUser = userRepository.save(otherUser);

        Cart cart = Cart.createCart(otherUser, testProduct1, 2);
        cart = cartRepository.save(cart);

        CreateOrderFromCartCommand command = new CreateOrderFromCartCommand(
                testUser.getId(),  // 다른 사용자 ID
                List.of(cart.getId()),
                null,
                null
        );

        // When & Then
        assertThrows(CartException.class, () -> createOrderFromCartUseCase.execute(command));
    }

    @Test
    @DisplayName("같은 상품이 여러 장바구니 항목에 있어도 정상적으로 처리된다")
    void createOrder_success_sameProductInMultipleCarts() {
        // Given
        Cart cart1 = Cart.createCart(testUser, testProduct1, 2);
        cart1 = cartRepository.save(cart1);

        Cart cart2 = Cart.createCart(testUser, testProduct1, 3);
        cart2 = cartRepository.save(cart2);

        CreateOrderFromCartCommand command = new CreateOrderFromCartCommand(
                testUser.getId(),
                List.of(cart1.getId(), cart2.getId()),
                null,
                null
        );

        int initialStock = testProduct1.getStock();

        // When
        CreateOrderResponse response = createOrderFromCartUseCase.execute(command);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.orderItems()).hasSize(2);

        // 재고가 총 5개(2+3) 감소했는지 확인
        Product updatedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(initialStock - 5);
        assertThat(updatedProduct.getSoldCount()).isEqualTo(5);
    }
}