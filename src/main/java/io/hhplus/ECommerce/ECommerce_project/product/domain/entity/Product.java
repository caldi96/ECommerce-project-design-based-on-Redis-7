package io.hhplus.ECommerce.ECommerce_project.product.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.common.entity.BaseEntity;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA를 위한 기본 생성자
@AllArgsConstructor(access = AccessLevel.PRIVATE)    // 정적 팩토리 메서드를 위한 private 생성자
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "sold_count", nullable = false)
    private int soldCount;

    @Column(name = "min_order_quantity")
    private Integer minOrderQuantity;

    @Column(name = "max_order_quantity")
    private Integer maxOrderQuantity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;  // 논리적 삭제용

    // ===== 정적 팩토리 메서드 =====

    /**
     * 상품 생성
     */
    public static Product createProduct(
        Category category,
        String name,
        String description,
        BigDecimal price,
        int stock,
        Integer minOrderQuantity,
        Integer maxOrderQuantity
    ) {
        validateName(name);
        validatePrice(price);
        validateStock(stock);

        if (minOrderQuantity != null && minOrderQuantity < 1) {
            throw new ProductException(ErrorCode.PRODUCT_MIN_ORDER_QUANTITY_INVALID);
        }

        if (maxOrderQuantity != null && maxOrderQuantity < 1) {
            throw new ProductException(ErrorCode.PRODUCT_MAX_ORDER_QUANTITY_INVALID);
        }

        if (minOrderQuantity != null && maxOrderQuantity != null && minOrderQuantity > maxOrderQuantity) {
            throw new ProductException(ErrorCode.PRODUCT_MIN_ORDER_QUANTITY_EXCEEDS_MAX);
        }

        return new Product(
            category,
            name,
            description,
            price,
            stock,
            true,       // isActive (초기 상태는 활성)
            0,          // viewCount
            0,          // soldCount
            minOrderQuantity,
            maxOrderQuantity,
            null,        // createdAt
            null,        // updatedAt
            null        // deletedAt (삭제되지 않음)
        );
    }

    /**
     * 캐시 DTO로부터 Product 생성 (인기 상품 조회용)
     * - Redis 캐시에서 조회한 데이터를 Product 엔티티로 변환
     * - Validation 없이 그대로 생성 (이미 검증된 데이터)
     */
    public static Product fromCache(
        Long id,
        Category category,
        String name,
        String description,
        BigDecimal price,
        int stock,
        boolean isActive,
        int viewCount,
        int soldCount,
        Integer minOrderQuantity,
        Integer maxOrderQuantity
    ) {
        Product product = new Product(
            category,
            name,
            description,
            price,
            stock,
            isActive,
            viewCount,
            soldCount,
            minOrderQuantity,
            maxOrderQuantity,
            null,  // createdAt
            null,  // updatedAt
            null   // deletedAt
        );
        product.setId(id);  // ID는 setter로 설정
        return product;
    }


    // ===== 비즈니스 로직 메서드 =====

    /**
     * 재고 차감 (상품 구매 시)
     */
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new ProductException(ErrorCode.PRODUCT_DECREASE_QUANTITY_INVALID);
        }

        if (this.stock < quantity) {
            throw new ProductException(ErrorCode.PRODUCT_OUT_OF_STOCK,
                "재고가 부족합니다. 현재 재고: " + this.stock + ", 요청 수량: " + quantity);
        }

        this.stock -= quantity;
    }

    /**
     * 재고 증가 (주문 취소, 반품 시)
     */
    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new ProductException(ErrorCode.PRODUCT_INCREASE_QUANTITY_INVALID);
        }

        this.stock += quantity;
    }

    /**
     * 재고 직접 설정 (재고 관리)
     */
    public void updateStock(int stock) {
        validateStock(stock);

        this.stock = stock;
    }

    /**
     * 판매량 증가
     */
    public void increaseSoldCount(int quantity) {
        if (quantity <= 0) {
            throw new ProductException(ErrorCode.PRODUCT_INCREASE_SOLD_COUNT_INVALID);
        }

        this.soldCount += quantity;
    }

    /**
     * 판매량 감소 (주문 취소 시)
     */
    public void decreaseSoldCount(int quantity) {
        if (quantity <= 0) {
            throw new ProductException(ErrorCode.PRODUCT_DECREASE_SOLD_COUNT_INVALID);
        }

        if (this.soldCount < quantity) {
            throw new ProductException(ErrorCode.PRODUCT_SOLD_COUNT_LESS_THAN_CANCEL,
                "판매량이 취소량보다 작습니다. 현재 판매량: " + this.soldCount + ", 취소량: " + quantity);
        }

        this.soldCount -= quantity;
    }

    /**
     * 조회수 증가
     */
    public void increaseViewCount() {
        this.viewCount++;
    }

    /**
     * 가격 수정
     */
    public void updatePrice(BigDecimal price) {
        validatePrice(price);

        this.price = price;
    }

    /**
     * 상품명 수정
     */
    public void updateName(String name) {
        validateName(name);

        this.name = name;
    }

    /**
     * 상품 설명 수정
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * 카테고리 수정
     */
    public void updateCategory(Category category) {
        this.category = category;
    }

    /**
     * 최소 주문량 수정
     */
    public void updateMinOrderQuantity(Integer minOrderQuantity) {
        if (minOrderQuantity != null && minOrderQuantity < 1) {
            throw new ProductException(ErrorCode.PRODUCT_MIN_ORDER_QUANTITY_INVALID);
        }

        if (minOrderQuantity != null && this.maxOrderQuantity != null && minOrderQuantity > this.maxOrderQuantity) {
            throw new ProductException(ErrorCode.PRODUCT_MIN_ORDER_QUANTITY_EXCEEDS_MAX);
        }

        this.minOrderQuantity = minOrderQuantity;
    }

    /**
     * 최대 주문량 수정
     */
    public void updateMaxOrderQuantity(Integer maxOrderQuantity) {
        if (maxOrderQuantity != null && maxOrderQuantity < 1) {
            throw new ProductException(ErrorCode.PRODUCT_MAX_ORDER_QUANTITY_INVALID);
        }

        if (maxOrderQuantity != null && this.minOrderQuantity != null && maxOrderQuantity < this.minOrderQuantity) {
            throw new ProductException(ErrorCode.PRODUCT_MAX_ORDER_QUANTITY_LESS_THAN_MIN);
        }

        this.maxOrderQuantity = maxOrderQuantity;
    }

    /**
     * 상품 활성화
     */
    public void activate() {
        if (this.isActive) {
            throw new ProductException(ErrorCode.PRODUCT_ALREADY_ACTIVE);
        }

        this.isActive = true;
    }

    /**
     * 상품 비활성화
     */
    public void deactivate() {
        if (!this.isActive) {
            throw new ProductException(ErrorCode.PRODUCT_ALREADY_INACTIVE);
        }

        this.isActive = false;
    }

    /**
     * 상품 삭제 (논리적 삭제)
     */
    public void delete() {
        if (this.deletedAt != null) {
            throw new ProductException(ErrorCode.PRODUCT_ALREADY_DELETED);
        }

        LocalDateTime now = LocalDateTime.now();
        this.deletedAt = now;
        this.isActive = false;  // 삭제 시 비활성화도 함께
    }

    // ===== 상태 확인 메서드 =====

    /**
     * 주문 가능 여부
     */
    public boolean canOrder(int quantity) {
        if (!this.isActive) {
            return false;  // 비활성 상품
        }

        if (this.stock < quantity) {
            return false;  // 재고 부족
        }

        if (this.minOrderQuantity != null && quantity < this.minOrderQuantity) {
            return false;  // 최소 주문량 미달
        }

        if (this.maxOrderQuantity != null && quantity > this.maxOrderQuantity) {
            return false;  // 최대 주문량 초과
        }

        return true;
    }

    /**
     * 주문 가능 여부 검증 (예외 발생)
     * - 주문이 불가능한 경우 구체적인 예외를 던짐
     */
    public void validateOrder(int quantity) {
        // 1. 비활성 상품 체크
        if (!this.isActive) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_ACTIVE,
                    " 비활성 상태의 상품은 주문할 수 없습니다.");
        }

        // 2. 재고 체크
        if (this.stock < quantity) {
            throw new ProductException(ErrorCode.PRODUCT_OUT_OF_STOCK,
                    " 현재 재고: " + this.stock + ", 요청 수량: " + quantity);
        }

        // 3. 최소 주문량 체크
        if (this.minOrderQuantity != null && quantity < this.minOrderQuantity) {
            throw new ProductException(ErrorCode.PRODUCT_MIN_ORDER_QUANTITY_NOT_MET,
                    " 최소 주문량: " + this.minOrderQuantity + ", 요청 수량: " + quantity);
        }

        // 4. 최대 주문량 체크
        if (this.maxOrderQuantity != null && quantity > this.maxOrderQuantity) {
            throw new ProductException(ErrorCode.PRODUCT_MAX_ORDER_QUANTITY_EXCEEDED,
                    " 최대 주문량: " + this.maxOrderQuantity + ", 요청 수량: " + quantity);
        }
    }

    /**
     * 재고 있음 여부
     */
    public boolean hasStock() {
        return this.stock > 0;
    }

    /**
     * 품절 여부 (계산 메서드)
     */
    public boolean isOutOfStock() {
        return this.stock == 0;
    }

    // ===== Validation 메서드 =====

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ProductException(ErrorCode.PRODUCT_NAME_REQUIRED);
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new ProductException(ErrorCode.PRODUCT_PRICE_REQUIRED);
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ProductException(ErrorCode.PRODUCT_PRICE_INVALID);
        }
    }

    private static void validateStock(int stock) {
        if (stock < 0) {
            throw new ProductException(ErrorCode.PRODUCT_STOCK_INVALID);
        }
    }
}
