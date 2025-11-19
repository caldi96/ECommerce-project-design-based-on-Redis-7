package io.hhplus.ECommerce.ECommerce_project.payment.application;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.OrderException;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.CreateOrderFromProductUseCase;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderResponse;
import io.hhplus.ECommerce.ECommerce_project.payment.application.command.CreatePaymentCommand;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.entity.Payment;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentMethod;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentStatus;
import io.hhplus.ECommerce.ECommerce_project.payment.infrastructure.PaymentRepository;
import io.hhplus.ECommerce.ECommerce_project.payment.presentation.response.CreatePaymentResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class CreatePaymentUseCaseIntegrationTest {

    @Autowired
    private CreatePaymentUseCase createPaymentUseCase;

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
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User testUser;
    private Category testCategory;
    private Product testProduct;

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

    @Test
    @DisplayName("정상적으로 결제가 완료된다 (쿠폰X, 포인트X)")
    void createPayment_success_withoutCouponAndPoint() {
        // Given - 주문 생성
        CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                3,
                null,
                null
        );
        CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);

        // When - 결제 생성
        CreatePaymentCommand paymentCommand = CreatePaymentCommand.of(
                orderResponse.orderId(),
                PaymentMethod.CARD
        );
        CreatePaymentResponse paymentResponse = createPaymentUseCase.execute(paymentCommand);

        // Then
        assertThat(paymentResponse).isNotNull();
        assertThat(paymentResponse.paymentId()).isNotNull();
        assertThat(paymentResponse.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(paymentResponse.paymentAmount()).isEqualTo(BigDecimal.valueOf(30000));
        assertThat(paymentResponse.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paymentResponse.paidAt()).isNotNull();

        // 결제 정보 확인
        Payment payment = paymentRepository.findById(paymentResponse.paymentId()).orElseThrow();
        assertThat(payment.isCompleted()).isTrue();
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);

        // 주문 상태 확인
        Orders order = orderRepository.findById(orderResponse.orderId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰을 사용한 주문에 대해 결제가 완료된다")
    void createPayment_success_withCoupon() {
        // Given - 쿠폰 생성 및 발급
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

        // 주문 생성 (30000원 - 5000원 할인 = 25000원)
        CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                3,
                null,
                coupon.getId()
        );
        CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);

        // When - 결제 생성
        CreatePaymentCommand paymentCommand = CreatePaymentCommand.of(
                orderResponse.orderId(),
                PaymentMethod.KAKAO_PAY
        );
        CreatePaymentResponse paymentResponse = createPaymentUseCase.execute(paymentCommand);

        // Then
        assertThat(paymentResponse).isNotNull();
        assertThat(paymentResponse.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(paymentResponse.paymentAmount()).isEqualTo(BigDecimal.valueOf(25000)); // 할인 적용된 금액

        // 주문 정보 확인
        Orders order = orderRepository.findById(orderResponse.orderId()).orElseThrow();
        assertThat(order.getDiscountAmount()).isEqualTo(BigDecimal.valueOf(5000));
        assertThat(order.getFinalAmount()).isEqualTo(BigDecimal.valueOf(25000));
    }

    @Test
    @DisplayName("포인트를 사용한 주문에 대해 결제가 완료된다")
    void createPayment_success_withPoint() {
        // Given - 포인트 충전
        Point point = Point.charge(testUser, BigDecimal.valueOf(5000), "테스트 충전");
        point = pointRepository.save(point);

        testUser.chargePoint(BigDecimal.valueOf(5000));
        testUser = userRepository.save(testUser);

        // 주문 생성 (20000원 - 3000원 포인트 = 17000원)
        CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                2,
                BigDecimal.valueOf(3000),
                null
        );
        CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);

        // When - 결제 생성
        CreatePaymentCommand paymentCommand = CreatePaymentCommand.of(
                orderResponse.orderId(),
                PaymentMethod.TOSS
        );
        CreatePaymentResponse paymentResponse = createPaymentUseCase.execute(paymentCommand);

        // Then
        assertThat(paymentResponse).isNotNull();
        assertThat(paymentResponse.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(paymentResponse.paymentAmount()).isEqualTo(BigDecimal.valueOf(20000)); // 포인트 포함된 금액

        // 주문 정보 확인
        Orders order = orderRepository.findById(orderResponse.orderId()).orElseThrow();
        assertThat(order.getPointAmount()).isEqualTo(BigDecimal.valueOf(3000));
        assertThat(order.getFinalAmount()).isEqualTo(BigDecimal.valueOf(20000));
    }

    @Test
    @DisplayName("쿠폰과 포인트를 모두 사용한 주문에 대해 결제가 완료된다")
    void createPayment_success_withCouponAndPoint() {
        // Given - 쿠폰 생성 및 발급
        Coupon coupon = Coupon.createCoupon(
                "20% 할인 쿠폰",
                "COUPON20",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(10000),
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

        // 주문 생성 (30000원 - 6000원 쿠폰 - 2000원 포인트 = 22000원)
        CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                3,
                BigDecimal.valueOf(2000),
                coupon.getId()
        );
        CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);

        // When - 결제 생성
        CreatePaymentCommand paymentCommand = CreatePaymentCommand.of(
                orderResponse.orderId(),
                PaymentMethod.BANK_TRANSFER
        );
        CreatePaymentResponse paymentResponse = createPaymentUseCase.execute(paymentCommand);

        // Then
        assertThat(paymentResponse).isNotNull();
        assertThat(paymentResponse.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(paymentResponse.paymentAmount()).isEqualTo(BigDecimal.valueOf(22000));

        // 주문 정보 확인
        Orders order = orderRepository.findById(orderResponse.orderId()).orElseThrow();
        assertThat(order.getTotalAmount()).isEqualTo(BigDecimal.valueOf(30000));
        assertThat(order.getDiscountAmount()).isEqualTo(BigDecimal.valueOf(6000));
        assertThat(order.getPointAmount()).isEqualTo(BigDecimal.valueOf(2000));
        assertThat(order.getFinalAmount()).isEqualTo(BigDecimal.valueOf(22000));
    }

    @Test
    @DisplayName("존재하지 않는 주문에 대해 결제가 실패한다")
    void createPayment_fail_orderNotFound() {
        // Given
        CreatePaymentCommand paymentCommand = CreatePaymentCommand.of(
                99999L,  // 존재하지 않는 주문 ID
                PaymentMethod.CARD
        );

        // When & Then
        assertThrows(OrderException.class, () -> createPaymentUseCase.execute(paymentCommand));
    }

    @Test
    @DisplayName("이미 결제된 주문에 대해 중복 결제가 실패한다")
    void createPayment_fail_alreadyPaid() {
        // Given - 주문 생성 및 결제 완료
        CreateOrderFromProductCommand orderCommand = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                2,
                null,
                null
        );
        CreateOrderResponse orderResponse = createOrderFromProductUseCase.execute(orderCommand);

        CreatePaymentCommand firstPaymentCommand = CreatePaymentCommand.of(
                orderResponse.orderId(),
                PaymentMethod.CARD
        );
        createPaymentUseCase.execute(firstPaymentCommand);

        // When & Then - 같은 주문에 대해 두 번째 결제 시도
        CreatePaymentCommand secondPaymentCommand = CreatePaymentCommand.of(
                orderResponse.orderId(),
                PaymentMethod.TOSS
        );
        assertThrows(OrderException.class, () -> createPaymentUseCase.execute(secondPaymentCommand));
    }

    @Test
    @DisplayName("여러 결제 수단으로 정상 결제된다")
    void createPayment_success_multiplePaymentMethods() {
        // CARD로 결제
        CreateOrderFromProductCommand orderCommand1 = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                1,
                null,
                null
        );
        CreateOrderResponse orderResponse1 = createOrderFromProductUseCase.execute(orderCommand1);
        CreatePaymentCommand paymentCommand1 = CreatePaymentCommand.of(orderResponse1.orderId(), PaymentMethod.CARD);
        CreatePaymentResponse paymentResponse1 = createPaymentUseCase.execute(paymentCommand1);
        assertThat(paymentResponse1.paymentMethod()).isEqualTo(PaymentMethod.CARD);

        // KAKAO_PAY로 결제
        CreateOrderFromProductCommand orderCommand2 = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                1,
                null,
                null
        );
        CreateOrderResponse orderResponse2 = createOrderFromProductUseCase.execute(orderCommand2);
        CreatePaymentCommand paymentCommand2 = CreatePaymentCommand.of(orderResponse2.orderId(), PaymentMethod.KAKAO_PAY);
        CreatePaymentResponse paymentResponse2 = createPaymentUseCase.execute(paymentCommand2);
        assertThat(paymentResponse2.paymentMethod()).isEqualTo(PaymentMethod.KAKAO_PAY);

        // TOSS로 결제
        CreateOrderFromProductCommand orderCommand3 = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                1,
                null,
                null
        );
        CreateOrderResponse orderResponse3 = createOrderFromProductUseCase.execute(orderCommand3);
        CreatePaymentCommand paymentCommand3 = CreatePaymentCommand.of(orderResponse3.orderId(), PaymentMethod.TOSS);
        CreatePaymentResponse paymentResponse3 = createPaymentUseCase.execute(paymentCommand3);
        assertThat(paymentResponse3.paymentMethod()).isEqualTo(PaymentMethod.TOSS);

        // BANK_TRANSFER로 결제
        CreateOrderFromProductCommand orderCommand4 = new CreateOrderFromProductCommand(
                testUser.getId(),
                testProduct.getId(),
                1,
                null,
                null
        );
        CreateOrderResponse orderResponse4 = createOrderFromProductUseCase.execute(orderCommand4);
        CreatePaymentCommand paymentCommand4 = CreatePaymentCommand.of(orderResponse4.orderId(), PaymentMethod.BANK_TRANSFER);
        CreatePaymentResponse paymentResponse4 = createPaymentUseCase.execute(paymentCommand4);
        assertThat(paymentResponse4.paymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
    }
}