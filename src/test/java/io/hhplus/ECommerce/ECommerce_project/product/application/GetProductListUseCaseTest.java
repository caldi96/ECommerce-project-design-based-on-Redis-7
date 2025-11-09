package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.common.SnowflakeIdGenerator;
import io.hhplus.ECommerce.ECommerce_project.product.application.dto.ProductPageResult;
import io.hhplus.ECommerce.ECommerce_project.product.application.enums.ProductSortType;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class GetProductListUseCaseTest {
    private ProductMemoryRepository productRepository;
    private GetProductListUseCase getProductListUseCase;

    @BeforeEach
    void setup() {
        productRepository = new ProductMemoryRepository(new SnowflakeIdGenerator());
        getProductListUseCase = new GetProductListUseCase(productRepository);

        // 테스트용 상품 10개 생성
        for (int i = 1; i <= 10; i++) {
            Product p = Product.createProduct(
                    "상품" + i,                     // name
                    1L,                            // categoryId
                    "설명" + i,
                    BigDecimal.valueOf(1000 + i),  // price
                    100,                           // stock
                    1,                             // minOrderQuantity
                    10                             // maxOrderQuantity
            );

            // 테스트 목적 데이터 세팅(정렬 확인용)
            setCreatedAt(p, LocalDateTime.now().minusDays(i));  // 최신순 정렬 테스트를 위해 createdAt 조작
            p.increaseSoldCount(i * 10);                       // 인기순 테스트
            setViewCount(p, i * 100);                    // 조회순 테스트

            productRepository.save(p);
        }
    }

    @Nested
    @DisplayName("상품 목록 조회 테스트")
    class ProductListRetrieveTest {

        @Test
        @DisplayName("최신순 정렬 + 페이징 정상 동작")
        void latestSortWithPaging() {
            ProductPageResult result = getProductListUseCase.execute(
                    1L,
                    ProductSortType.LATEST,
                    0,
                    5
            );

            assertThat(result.getProducts()).hasSize(5);
            assertThat(result.getTotalElements()).isEqualTo(10);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isFalse();

            // 최신순: createdAt DESC → i=1 이 가장 최신
            assertThat(result.getProducts().get(0).getName()).isEqualTo("상품1");
        }

        @Test
        @DisplayName("가격 낮은순 정렬")
        void priceLowSort() {
            ProductPageResult result = getProductListUseCase.execute(
                    1L,
                    ProductSortType.PRICE_LOW,
                    0,
                    10
            );

            assertThat(result.getProducts().get(0).getPrice()).isEqualTo(BigDecimal.valueOf(1001));
            assertThat(result.getProducts().get(9).getPrice()).isEqualTo(BigDecimal.valueOf(1010));
        }

        @Test
        @DisplayName("판매량 높은순 정렬 검증")
        void popularSort() {
            ProductPageResult result = getProductListUseCase.execute(
                    1L,
                    ProductSortType.POPULAR,
                    0,
                    3
            );

            // soldCount: 1→10, 2→20, ..., 10→100 → 인기순은 상품10이 1등
            assertThat(result.getProducts().get(0).getSoldCount()).isEqualTo(100);
            assertThat(result.getProducts().get(2).getSoldCount()).isEqualTo(80);
        }

        @Test
        @DisplayName("조회수 높은순 정렬 검증")
        void viewedSort() {
            ProductPageResult result = getProductListUseCase.execute(
                    1L,
                    ProductSortType.VIEWED,
                    0,
                    3
            );

            // viewCount: i * 100 → 상품10이 1000, 상품9가 900
            assertThat(result.getProducts().get(0).getViewCount()).isEqualTo(1000);
            assertThat(result.getProducts().get(1).getViewCount()).isEqualTo(900);
        }

        @Test
        @DisplayName("페이징 마지막 페이지 검사")
        void lastPageCheck() {
            ProductPageResult result = getProductListUseCase.execute(
                    1L,
                    ProductSortType.LATEST,
                    1,
                    5
            );

            assertThat(result.isLast()).isTrue();
            assertThat(result.getProducts()).hasSize(5);
        }
    }

    private void setCreatedAt(Product product, LocalDateTime time) {
        try {
            Field field = Product.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(product, time);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setViewCount(Product product, int count) {
        try {
            Field field = Product.class.getDeclaredField("viewCount");
            field.setAccessible(true);
            field.set(product, count);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
