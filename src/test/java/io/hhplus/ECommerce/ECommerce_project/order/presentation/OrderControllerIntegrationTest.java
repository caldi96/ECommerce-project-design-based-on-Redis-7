package io.hhplus.ECommerce.ECommerce_project.order.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.infrastructure.CartRepository;
import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.request.CreateOrderFromCartRequest;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.request.CreateOrderFromProductRequest;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private PointUsageHistoryRepository pointUsageHistoryRepository;

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
        // 테스트 사용자 생성 (포인트 0으로 시작)
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

    @AfterEach
    void tearDown() {
        // 외래 키 제약조건을 고려한 순서로 삭제
        // 1. 주문 관련 삭제 (자식 먼저)
        pointUsageHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();

        // 2. 장바구니 삭제
        cartRepository.deleteAll();

        // 3. 포인트 삭제
        pointRepository.deleteAll();

        // 4. 쿠폰 관련 삭제 (사용자 쿠폰 먼저)
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();

        // 5. 상품 삭제
        productRepository.deleteAll();

        // 6. 카테고리 삭제
        categoryRepository.deleteAll();

        // 7. 사용자 삭제
        userRepository.deleteAll();
    }

    // ========== POST /api/orders/from-cart 테스트 ==========

    @Test
    @DisplayName("[E2E] POST /api/orders/from-cart - 정상적으로 장바구니에서 주문이 생성된다")
    void createOrderFromCart_success() throws Exception {
        // Given
        Cart cart1 = Cart.createCart(testUser, testProduct1, 2);
        cart1 = cartRepository.save(cart1);

        Cart cart2 = Cart.createCart(testUser, testProduct2, 1);
        cart2 = cartRepository.save(cart2);

        CreateOrderFromCartRequest request = new CreateOrderFromCartRequest(
                testUser.getId(),
                List.of(cart1.getId(), cart2.getId()),
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.orderItems").isArray())
                .andExpect(jsonPath("$.orderItems", hasSize(2)))
                .andExpect(jsonPath("$.totalAmount").value(40000))  // 상품 총액 (상품1: 20000 + 상품2: 20000)
                .andExpect(jsonPath("$.shippingFee").value(0))      // 배송비 (40000원 이상이므로 무료 배송)
                .andExpect(jsonPath("$.finalAmount").value(40000)); // 최종 금액
    }

    @Test
    @DisplayName("[E2E] POST /api/orders/from-cart - 쿠폰을 사용하여 주문이 생성된다")
    void createOrderFromCart_withCoupon() throws Exception {
        // Given
        Cart cart = Cart.createCart(testUser, testProduct1, 2);
        cart = cartRepository.save(cart);

        // 10% 할인 쿠폰
        Coupon coupon = Coupon.createCoupon(
                "10% 할인 쿠폰",
                "COUPON10",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(10000),
                100,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        coupon = couponRepository.save(coupon);

        UserCoupon userCoupon = UserCoupon.issueCoupon(testUser, coupon);
        userCouponRepository.save(userCoupon);

        CreateOrderFromCartRequest request = new CreateOrderFromCartRequest(
                testUser.getId(),
                List.of(cart.getId()),
                null,
                coupon.getId()
        );

        // When & Then
        mockMvc.perform(post("/api/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.discountAmount").value(2000)); // 20000 * 10%
    }

    @Test
    @DisplayName("[E2E] POST /api/orders/from-cart - 포인트를 사용하여 주문이 생성된다")
    void createOrderFromCart_withPoint() throws Exception {
        // Given
        Cart cart = Cart.createCart(testUser, testProduct1, 2);
        cart = cartRepository.save(cart);

        // 포인트 충전
        Point point = Point.charge(testUser, BigDecimal.valueOf(5000), "테스트 충전");
        pointRepository.save(point);

        testUser.chargePoint(BigDecimal.valueOf(5000));
        userRepository.save(testUser);

        CreateOrderFromCartRequest request = new CreateOrderFromCartRequest(
                testUser.getId(),
                List.of(cart.getId()),
                BigDecimal.valueOf(3000),
                null
        );

        // When & Then
        mockMvc.perform(post("/api/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.pointAmount").value(3000));
    }

    @Test
    @DisplayName("[E2E] POST /api/orders/from-cart - userId가 null이면 400 Bad Request")
    void createOrderFromCart_validation_userIdNull() throws Exception {
        // Given
        CreateOrderFromCartRequest request = new CreateOrderFromCartRequest(
                null,  // userId null
                List.of(1L),
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[E2E] POST /api/orders/from-cart - cartItemIds가 빈 배열이면 400 Bad Request")
    void createOrderFromCart_validation_emptyCartItems() throws Exception {
        // Given
        CreateOrderFromCartRequest request = new CreateOrderFromCartRequest(
                testUser.getId(),
                List.of(),  // 빈 배열
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ========== POST /api/orders/from-product 테스트 ==========

    @Test
    @DisplayName("[E2E] POST /api/orders/from-product - 정상적으로 상품에서 주문이 생성된다")
    void createOrderFromProduct_success() throws Exception {
        // Given
        CreateOrderFromProductRequest request = new CreateOrderFromProductRequest(
                testUser.getId(),
                testProduct1.getId(),
                3,
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/orders/from-product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.orderItems").isArray())
                .andExpect(jsonPath("$.orderItems", hasSize(1)))
                .andExpect(jsonPath("$.totalAmount").value(30000))  // 상품 총액 (10000 × 3)
                .andExpect(jsonPath("$.shippingFee").value(0))      // 배송비 (30000원 이상이므로 무료 배송)
                .andExpect(jsonPath("$.finalAmount").value(30000)); // 최종 금액
    }

    @Test
    @DisplayName("[E2E] POST /api/orders/from-product - 쿠폰과 포인트를 모두 사용하여 주문이 생성된다")
    void createOrderFromProduct_withCouponAndPoint() throws Exception {
        // Given
        // 5000원 할인 쿠폰
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
        userCouponRepository.save(userCoupon);

        // 포인트 충전
        Point point = Point.charge(testUser, BigDecimal.valueOf(10000), "테스트 충전");
        pointRepository.save(point);

        testUser.chargePoint(BigDecimal.valueOf(10000));
        userRepository.save(testUser);

        CreateOrderFromProductRequest request = new CreateOrderFromProductRequest(
                testUser.getId(),
                testProduct1.getId(),
                3,  // 30000원
                BigDecimal.valueOf(2000),
                coupon.getId()
        );

        // When & Then
        mockMvc.perform(post("/api/orders/from-product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.totalAmount").value(30000))  // 상품 총액 (10000 × 3)
                .andExpect(jsonPath("$.shippingFee").value(0))      // 배송비 (30000원 이상이므로 무료 배송)
                .andExpect(jsonPath("$.discountAmount").value(5000))
                .andExpect(jsonPath("$.pointAmount").value(2000))
                .andExpect(jsonPath("$.finalAmount").value(23000)); // 30000 - 5000 - 2000
    }

    @Test
    @DisplayName("[E2E] POST /api/orders/from-product - quantity가 0이면 400 Bad Request")
    void createOrderFromProduct_validation_quantityZero() throws Exception {
        // Given
        CreateOrderFromProductRequest request = new CreateOrderFromProductRequest(
                testUser.getId(),
                testProduct1.getId(),
                0,  // 0은 @Min(1) 위반
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/orders/from-product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[E2E] POST /api/orders/from-product - productId가 null이면 400 Bad Request")
    void createOrderFromProduct_validation_productIdNull() throws Exception {
        // Given
        CreateOrderFromProductRequest request = new CreateOrderFromProductRequest(
                testUser.getId(),
                null,  // productId null
                1,
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/orders/from-product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[E2E] POST /api/orders/from-product - 재고 부족 시 예외 발생")
    void createOrderFromProduct_outOfStock() throws Exception {
        // Given
        testProduct1.updateStock(2);  // 재고를 2개로 줄임
        productRepository.save(testProduct1);

        CreateOrderFromProductRequest request = new CreateOrderFromProductRequest(
                testUser.getId(),
                testProduct1.getId(),
                10,  // 재고(2)보다 많은 수량
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/orders/from-product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is4xxClientError());  // ProductException 발생
    }

    // ========== GET /api/orders/{orderId} 테스트 ==========

    @Test
    @DisplayName("[E2E] GET /api/orders/{orderId} - 주문 상세 조회가 성공한다")
    void getOrderDetail_success() throws Exception {
        // Given: 먼저 주문 생성
        Cart cart = Cart.createCart(testUser, testProduct1, 1);
        cart = cartRepository.save(cart);

        CreateOrderFromCartRequest createRequest = new CreateOrderFromCartRequest(
                testUser.getId(),
                List.of(cart.getId()),
                null,
                null
        );

        String createResponse = mockMvc.perform(post("/api/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(createResponse).get("orderId").asLong();

        // When & Then: 주문 상세 조회
        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .param("userId", testUser.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.orderItems").isArray())
                .andExpect(jsonPath("$.orderItems", hasSize(greaterThan(0))));
    }

    // ========== GET /api/orders 테스트 ==========

    @Test
    @DisplayName("[E2E] GET /api/orders - 사용자별 주문 목록 조회가 성공한다")
    void getOrderList_success() throws Exception {
        // Given: 주문 2개 생성
        for (int i = 0; i < 2; i++) {
            Cart cart = Cart.createCart(testUser, testProduct1, 1);
            cart = cartRepository.save(cart);

            CreateOrderFromCartRequest request = new CreateOrderFromCartRequest(
                    testUser.getId(),
                    List.of(cart.getId()),
                    null,
                    null
            );

            mockMvc.perform(post("/api/orders/from-cart")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        // When & Then
        mockMvc.perform(get("/api/orders")
                        .param("userId", testUser.getId().toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(2)));
    }
}