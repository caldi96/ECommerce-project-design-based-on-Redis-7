# 동시성 테스트 재작성 분석 문서

## 1. 엔티티 파일 분석

### 1.1 User 엔티티
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/user/domain/entity/User.java`

**주요 특성**:
- `@Version` 사용: O (낙관적 락 적용)
  - `private Long version;` (라인 43)
- 생성자: `@NoArgsConstructor`, `@AllArgsConstructor`
- 필수 필드:
  - `username` (unique, 로그인 id)
  - `password`
  - `pointBalance` (BigDecimal, default: ZERO)
- 주요 메서드:
  - `usePoint(BigDecimal amount)` - 포인트 사용
  - `chargePoint(BigDecimal amount)` - 포인트 충전
  - `refundPoint(BigDecimal amount)` - 포인트 환불
  - `hasEnoughPoint(BigDecimal amount)` - 포인트 충분 여부 확인

---

### 1.2 Product 엔티티
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/product/domain/entity/Product.java`

**주요 특성**:
- `@Version` 사용: X (낙관적 락 미적용)
- 생성자: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)`
- 정적 팩토리 메서드: `createProduct()` (라인 70-110)
- 필수 필드:
  - `category` (ManyToOne)
  - `name`
  - `price` (BigDecimal)
  - `stock` (int)
  - `isActive` (boolean)
- 주요 메서드:
  - `decreaseStock(int quantity)` - 재고 차감
  - `increaseStock(int quantity)` - 재고 증가
  - `updateStock(int stock)` - 재고 직접 설정
  - `increaseSoldCount(int quantity)` - 판매량 증가
  - `validateOrder(int quantity)` - 주문 가능 여부 검증
  - `canOrder(int quantity)` - 주문 가능 여부 확인

---

### 1.3 Category 엔티티
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/category/domain/entity/Category.java`

**주요 특성**:
- `@Version` 사용: X
- 생성자: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)`
- 정적 팩토리 메서드: `createCategory()` (라인 47-61)
- 필수 필드:
  - `categoryName` (unique)
  - `displayOrder` (int)
- 주요 메서드:
  - `updateCategoryName(String name)` - 카테고리명 수정
  - `updateDisplayOrder(int displayOrder)` - 표시 순서 변경
  - `delete()` - 논리적 삭제
  - `isDeleted()` - 삭제 여부 확인

---

### 1.4 Coupon 엔티티
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/coupon/domain/entity/Coupon.java`

**주요 특성**:
- `@Version` 사용: X (낙관적 락 미적용)
- 생성자: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)`
- 정적 팩토리 메서드: `createCoupon()` (라인 80-143)
- 필수 필드:
  - `name`
  - `code` (unique)
  - `discountType` (DiscountType enum)
  - `discountValue` (BigDecimal)
  - `totalQuantity` (int, 발급 제한)
  - `issuedQuantity` (int, 현재 발급된 수량)
  - `perUserLimit` (int, 사용자당 제한)
- 주요 메서드:
  - `increaseIssuedQuantity()` - 발급 수량 증가 (선착순 제어)
  - `hasRemainingQuantity()` - 발급 가능 여부
  - `validateAvailability(LocalDateTime now)` - 유효성 검증
  - `calculateDiscountAmount(BigDecimal orderAmount)` - 할인 금액 계산

---

### 1.5 UserCoupon 엔티티
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/coupon/domain/entity/UserCoupon.java`

**주요 특성**:
- `@Version` 사용: O (낙관적 락 적용)
  - `private Long version;` (라인 33)
- 생성자: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)`
- 정적 팩토리 메서드: `issueCoupon(User user, Coupon coupon)` (라인 64-76)
- 고유 제약: `uk_user_coupon` (user_id + coupon_id 조합이 unique)
- 필수 필드:
  - `coupon` (ManyToOne)
  - `user` (ManyToOne)
  - `status` (UserCouponStatus enum: AVAILABLE, USED, EXPIRED)
  - `usedCount` (int)
- 주요 메서드:
  - `use(int perUserLimit)` - 쿠폰 사용
  - `cancelUse(int perUserLimit)` - 쿠폰 사용 취소
  - `validateCanUse(int perUserLimit)` - 사용 가능 여부 검증
  - `expire()` - 쿠폰 만료 처리

---

### 1.6 Orders 엔티티
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/order/domain/entity/Orders.java`

**주요 특성**:
- `@Version` 사용: X
- 생성자: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)`
- 정적 팩토리 메서드: `createOrder()` (라인 71-118)
- 필수 필드:
  - `user` (ManyToOne)
  - `coupon` (ManyToOne, nullable)
  - `totalAmount` (BigDecimal)
  - `discountAmount` (BigDecimal)
  - `finalAmount` (BigDecimal)
  - `shippingFee` (BigDecimal)
  - `status` (OrderStatus enum)
  - `pointAmount` (BigDecimal)
- 주요 메서드:
  - `paid()` - 결제 완료 처리 (PENDING → PAID)
  - `cancel()` - 주문 취소 처리
  - `cancelAfterPaid()` - 결제 후 취소
  - `complete()` - 주문 완료 처리
  - `paymentFailed()` - 결제 실패 처리

---

### 1.7 OrderItem 엔티티
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/order/domain/entity/OrderItem.java`

**주요 특성**:
- `@Version` 사용: X
- 생성자: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)`
- 정적 팩토리 메서드: `createOrderItem()` (라인 73-104)
- 필수 필드:
  - `product` (ManyToOne)
  - `orders` (ManyToOne)
  - `productName` (String)
  - `quantity` (int)
  - `unitPrice` (BigDecimal)
  - `subTotal` (BigDecimal)
  - `status` (OrderItemStatus enum)
- 주요 메서드:
  - `complete()` - 항목 완료 처리
  - `cancel()` - 항목 취소
  - `returnItem()` - 반품 처리
  - `refund()` - 환불 처리

---

### 1.8 Payment 엔티티
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/payment/domain/entity/Payment.java`

**주요 특성**:
- `@Version` 사용: X
- 생성자: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)`
- 정적 팩토리 메서드: 
  - `createPayment()` (라인 70-86) - 결제 생성
  - `createRefund()` (라인 91-107) - 환불 생성
- 필수 필드:
  - `order` (ManyToOne)
  - `amount` (BigDecimal)
  - `paymentType` (PaymentType enum: PAYMENT, REFUND)
  - `paymentMethod` (PaymentMethod enum: CARD, BANK_TRANSFER, KAKAO_PAY, TOSS)
  - `paymentStatus` (PaymentStatus enum: PENDING, COMPLETED, FAILED, REFUNDED)
- 주요 메서드:
  - `complete()` - 결제 완료 처리
  - `fail(String reason)` - 결제 실패 처리
  - `refund()` - 환불 처리

---

### 1.9 Point 엔티티
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/point/domain/entity/Point.java`

**주요 특성**:
- `@Version` 사용: O (낙관적 락 적용)
  - `private Long version;` (라인 25)
- 생성자: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)`
- 정적 팩토리 메서드:
  - `charge()` (라인 72-92) - 포인트 충전
  - `refund()` (라인 97-117) - 포인트 환불
- 필수 필드:
  - `user` (ManyToOne)
  - `amount` (BigDecimal)
  - `usedAmount` (BigDecimal, 부분 사용 지원)
  - `pointType` (PointType enum: CHARGE, REFUND)
  - `isUsed` (boolean)
  - `isExpired` (boolean)
- 주요 메서드:
  - `usePartially(BigDecimal amountToUse)` - 부분 사용
  - `restoreUsedAmount(BigDecimal amountToRestore)` - 사용 취소
  - `getRemainingAmount()` - 남은 금액 계산
  - `isAvailable()` - 사용 가능 여부

---

### 1.10 PointUsageHistory 엔티티
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/point/domain/entity/PointUsageHistory.java`

**주요 특성**:
- `@Version` 사용: X
- 생성자: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)`
- 정적 팩토리 메서드: `create()` (라인 49-66)
- 필수 필드:
  - `point` (ManyToOne)
  - `orders` (ManyToOne)
  - `usedAmount` (BigDecimal)
- 주요 메서드:
  - `cancel()` - 포인트 사용 취소
  - `isCanceled()` - 취소 여부 확인

---

## 2. Enum 파일 분석

### 2.1 DiscountType
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/coupon/domain/enums/DiscountType.java`

```java
public enum DiscountType {
    PERCENTAGE("정률할인"),    // 정률 할인 (%)
    FIXED("정액할인");        // 정액 할인
}
```

---

### 2.2 OrderStatus
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/order/domain/enums/OrderStatus.java`

```java
public enum OrderStatus {
    PENDING("주문중"),           // 결제 대기
    PAID("결제완료"),           // 결제 완료
    COMPLETED("주문완료"),       // 주문 완료
    PAYMENT_FAILED("결제실패"),   // 결제 실패
    CANCELED("주문취소");        // 주문 취소
}
```

---

### 2.3 PaymentStatus
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/payment/domain/enums/PaymentStatus.java`

```java
public enum PaymentStatus {
    PENDING("결제대기"),      // 결제 대기
    COMPLETED("결제완료"),    // 결제 완료
    FAILED("결제실패"),       // 결제 실패
    REFUNDED("환불완료");    // 환불 완료
}
```

---

### 2.4 PaymentMethod
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/payment/domain/enums/PaymentMethod.java`

```java
public enum PaymentMethod {
    CARD("신용카드"),
    BANK_TRANSFER("계좌이체"),
    KAKAO_PAY("카카오페이"),
    TOSS("토스");
}
```

---

## 3. Command 파일 분석

### 3.1 IssueCouponCommand
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/coupon/application/command/IssueCouponCommand.java`

```java
public record IssueCouponCommand(
    Long userId,
    Long couponId
) {}
```

**필드**:
- `userId` - 쿠폰을 발급받을 사용자 ID
- `couponId` - 발급받을 쿠폰 ID

---

### 3.2 CreateOrderFromProductCommand
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/order/application/command/CreateOrderFromProductCommand.java`

```java
public record CreateOrderFromProductCommand(
    Long userId,
    Long productId,           // 주문할 상품 ID
    Integer quantity,         // 주문 수량
    BigDecimal pointAmount,   // 사용할 포인트 (선택, null 가능)
    Long couponId            // 사용할 쿠폰 ID (선택, null 가능)
) {}
```

**필드**:
- `userId` - 주문자 ID
- `productId` - 상품 ID
- `quantity` - 주문 수량
- `pointAmount` - 사용할 포인트 (optional)
- `couponId` - 사용할 쿠폰 ID (optional)

---

### 3.3 CreatePaymentCommand
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/payment/application/command/CreatePaymentCommand.java`

```java
public record CreatePaymentCommand(
    Long orderId,
    PaymentMethod paymentMethod
) {
    public static CreatePaymentCommand of(Long orderId, PaymentMethod paymentMethod) {
        return new CreatePaymentCommand(orderId, paymentMethod);
    }
}
```

**필드**:
- `orderId` - 주문 ID
- `paymentMethod` - 결제 방법

---

## 4. UseCase/Service 파일 분석

### 4.1 IssueCouponUseCase
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/coupon/application/IssueCouponUseCase.java`

**메서드 시그니처**:
```java
@Transactional
public UserCoupon execute(IssueCouponCommand command)
```

**동작 흐름** (라인 39-71):
1. 사용자 ID, 쿠폰 ID 검증
2. 사용자 조회 (락 없이)
3. 빠른 중복 체크 (락 없이, Double-Check Locking 첫 번째)
4. 쿠폰 조회 (비관적 락 - 선착순 보장)
5. 쿠폰 유효성 검증
6. 중복 체크 다시 (Double-Check Locking 두 번째)
7. 쿠폰 발급 수량 증가
8. UserCoupon 생성 및 저장
9. DB 유니크 제약으로 최종 안전장치

**동시성 제어**:
- 비관적 락 (PESSIMISTIC_WRITE) 사용 - getCouponWithLock()
- Double-Check Locking 패턴
- DB 유니크 제약 (uk_user_coupon)

---

### 4.2 CreateOrderFromProductUseCase
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/order/application/CreateOrderFromProductUseCase.java`

**메서드 시그니처**:
```java
public CreateOrderResponse execute(CreateOrderFromProductCommand command)
```

**동작 흐름** (라인 50-66):
1. 검증 및 사전 계산 (트랜잭션 밖) - `validateAndCalculate()`
2. 재고 차감 (트랜잭션 1) - `stockService.reserveStock()`
3. 주문 완료 (트랜잭션 2) - `completeOrder()`
4. 실패 시 재고 복구 (보상 트랜잭션) - `stockService.compensateStock()`

**내부 메서드: completeOrder()** (라인 146-268):
```java
@Retryable(
    retryFor = OptimisticLockingFailureException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 100)
)
@Transactional
public CreateOrderResponse completeOrder(
    CreateOrderFromProductCommand command,
    ValidatedOrderFromProductData validatedOrderFromProductData
)
```

**동시성 제어**:
- 낙관적 락 (User, Point, UserCoupon) - @Version 필드
- 보상 트랜잭션 (Saga Pattern)
- @Retryable로 OptimisticLockingFailureException 처리
- 재고 예약 메커니즘 (별도 트랜잭션)

---

### 4.3 CreatePaymentUseCase
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/payment/application/CreatePaymentUseCase.java`

**메서드 시그니처**:
```java
@Transactional
public CreatePaymentResponse execute(CreatePaymentCommand command)
```

**동작 흐름** (라인 43-90):
1. 주문 조회 (비관적 락)
2. 주문 상태 검증 (PENDING만 결제 가능)
3. 결제 정보 생성
4. 결제 처리 (항상 성공으로 가정)
5. 주문 상태를 PAID로 변경
6. 실패 시 보상 트랜잭션 호출

**보상 트랜잭션: rollbackOrderResources()** (라인 96-179):
- 상품 재고 복구 (비관적 락)
- 쿠폰 사용 취소 (비관적 락)
- 포인트 복구 (비관적 락)
- User 포인트 환불 (비관적 락)

**동시성 제어**:
- 비관적 락 (PESSIMISTIC_WRITE) - Orders, Product, UserCoupon, Point, User
- 보상 트랜잭션 (Saga Pattern)

---

## 5. Repository 파일 분석

### 5.1 UserRepository
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/user/infrastructure/UserRepository.java`

**메서드**:
- `findByIdWithLock(Long userId)` - 비관적 락 (PESSIMISTIC_WRITE)

---

### 5.2 ProductRepository
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/product/infrastructure/ProductRepository.java`

**메서드**:
- `findByIdActive(Long id)` - 삭제되지 않은 상품만 조회
- `findByIdWithLock(Long id)` - 비관적 락 (PESSIMISTIC_WRITE)
- `countActiveProducts(Long categoryId)` - 활성 상품 개수
- `findByIsActiveTrueAndDeletedAtIsNull()` - 활성 상품 목록
- `findProducts(Long categoryId, Pageable pageable)` - 페이징 조회

---

### 5.3 CouponRepository
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/coupon/infrastructure/CouponRepository.java`

**메서드**:
- `findByCodeIgnoreCase(String code)` - 코드로 조회
- `findAllAvailableCoupons()` - 사용 가능 쿠폰 목록
- `findByIdWithLock(Long couponId)` - 비관적 락 (PESSIMISTIC_WRITE)

---

### 5.4 UserCouponRepository
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/coupon/infrastructure/UserCouponRepository.java`

**메서드**:
- `findByUser_IdAndCoupon_Id(Long userId, Long couponId)` - 기본 조회
- `findByUser_IdAndCoupon_IdWithLock(Long userId, Long couponId)` - 비관적 락
- `findByUser_Id(Long userId)` - 사용자 쿠폰 목록
- `findByUser_IdAndStatus(Long userId, UserCouponStatus status)` - 상태별 조회

---

### 5.5 OrderRepository
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/order/infrastructure/OrderRepository.java`

**메서드**:
- `findByUserIdWithPaging(Long userId, OrderStatus orderStatus, Pageable pageable)` - 페이징 조회
- `countByUserId(Long userId, OrderStatus orderStatus)` - 주문 개수
- `findByIdWithLock(Long orderId)` - 비관적 락 (PESSIMISTIC_WRITE)

---

### 5.6 PaymentRepository
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/payment/infrastructure/PaymentRepository.java`

**메서드**:
- `findByOrder_Id(Long orderId)` - 주문 ID로 결제 목록 조회

---

### 5.7 OrderItemRepository
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/order/infrastructure/OrderItemRepository.java`

**메서드**:
- `findByOrders_Id(Long orderId)` - 주문 ID로 항목 조회

---

### 5.8 PointRepository
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/point/infrastructure/PointRepository.java`

**메서드**:
- `findAvailablePointsByUserId(Long userId)` - 사용 가능 포인트 조회
- `findByUserIdWithPaging(Long userId, Pageable pageable)` - 페이징 조회
- `countByUserIdAndDeletedAtIsNull(Long userId)` - 포인트 개수
- `findByIdWithLock(Long pointId)` - 비관적 락 (PESSIMISTIC_WRITE)

---

### 5.9 PointUsageHistoryRepository
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/main/java/io/hhplus/ECommerce/ECommerce_project/point/infrastructure/PointUsageHistoryRepository.java`

**메서드**:
- `findByOrders_IdAndCanceledAtIsNull(Long orderId)` - 주문 ID로 미취소 사용 이력 조회
- `findByPoint_IdAndCanceledAtIsNull(Long pointId)` - 포인트 ID로 미취소 사용 이력 조회

---

## 6. 동시성 제어 전략 요약

### 6.1 낙관적 락 (@Version) 적용 엔티티
- **User** - 포인트 잔액 변경
- **UserCoupon** - 쿠폰 사용 횟수 변경
- **Point** - 포인트 사용량 변경

### 6.2 비관적 락 (PESSIMISTIC_WRITE) 적용 Repository
- **UserRepository.findByIdWithLock()** - User 조회 시 락
- **ProductRepository.findByIdWithLock()** - Product 조회 시 락 (재고 차감)
- **CouponRepository.findByIdWithLock()** - Coupon 조회 시 락 (선착순)
- **UserCouponRepository.findByUser_IdAndCoupon_IdWithLock()** - UserCoupon 조회 시 락
- **OrderRepository.findByIdWithLock()** - Orders 조회 시 락 (결제 처리)
- **PointRepository.findByIdWithLock()** - Point 조회 시 락

### 6.3 기타 동시성 제어
- **Double-Check Locking** - IssueCouponUseCase에서 중복 발급 방지
- **DB 유니크 제약** - UserCoupon (user_id + coupon_id)
- **보상 트랜잭션 (Saga Pattern)** - CreatePaymentUseCase에서 실패 시 롤백
- **@Retryable** - CreateOrderFromProductUseCase에서 OptimisticLockingFailureException 처리

---

## 7. 기존 동시성 테스트 참고사항

### 7.1 CouponConcurrencyTest
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/test/java/io/hhplus/ECommerce/ECommerce_project/integration/concurrency/CouponConcurrencyTest.java`

**테스트 구조**:
- `@SpringBootTest` + `@ActiveProfiles("integration")`
- `setUp()` 메서드에서 데이터 정리 및 초기화
- `ExecutorService` + `CountDownLatch` 사용한 동시 실행
- `AtomicInteger` 사용한 성공/실패 카운팅

**주요 검증**:
- 쿠폰 발급 제한 수량 초과 방지
- 사용자당 1개만 발급
- 발급 수량의 정확성

---

### 7.2 StockConcurrencyTest
**파일경로**: `/Volumes/E 드라이브/study/ECommerce-project/src/test/java/io/hhplus/ECommerce/ECommerce_project/integration/concurrency/StockConcurrencyTest.java`

**테스트 구조**:
- `@SpringBootTest` + `@ActiveProfiles("integration")`
- `setUp()` 메서드에서 기존 데이터 정리 (외래 키 제약 고려)
- 순서: OrderItem → Order → Product → User → Category
- 미리 사용자 생성 (배열)

**주요 검증**:
- 재고 정확한 감소
- 재고 부족 시 처리
- 동시 주문 안정성

---

## 8. 동시성 테스트 재작성 시 필수 고려사항

### 8.1 테스트 케이스 작성
1. **IssueCouponUseCase**
   - 100명의 사용자가 동시에 10개 제한 쿠폰 발급
   - 예상: 10개만 성공, 90개 실패
   - 검증: 발급된 쿠폰 수, 각 사용자당 최대 1개

2. **CreateOrderFromProductUseCase**
   - 100명의 사용자가 동시에 재고 50개 상품 주문
   - 각자 1개씩 주문 시 50명만 성공
   - 검증: 주문 성공 수, 재고 정확성, 보상 트랜잭션 작동

3. **CreatePaymentUseCase**
   - 결제 중 동시 접근 시 안정성
   - 보상 트랜잭션 정확성 (재고, 쿠폰, 포인트 복구)

### 8.2 데이터 정리 순서 (외래 키 제약 고려)
```
PointUsageHistory 삭제
OrderItem 삭제
Order 삭제
Payment 삭제
Point 삭제
UserCoupon 삭제
Product 삭제
Coupon 삭제
Category 삭제
User 삭제
```

### 8.3 스레드 풀 관리
```java
ExecutorService executor = Executors.newFixedThreadPool(threadCount);
CountDownLatch startLatch = new CountDownLatch(1);  // 동시 시작
CountDownLatch completionLatch = new CountDownLatch(threadCount);  // 완료 대기
```

### 8.4 검증 방법
```java
// Refresh to get latest state from DB
refresh(entity);
assertThat(entity.getField()).isEqualTo(expectedValue);
```

### 8.5 에러 처리
```java
AtomicInteger successCount = new AtomicInteger(0);
AtomicInteger failureCount = new AtomicInteger(0);

try {
    // 비즈니스 로직
    successCount.incrementAndGet();
} catch (Exception e) {
    failureCount.incrementAndGet();
}

// 최종 검증
assertThat(successCount.get()).isEqualTo(expectedSuccesses);
assertThat(failureCount.get()).isEqualTo(expectedFailures);
```

