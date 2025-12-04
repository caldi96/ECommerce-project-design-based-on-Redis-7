package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.category.application.service.CategoryFinderService;
import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductRedisCacheService;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.RedisRankingService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetTopRankedProductsUseCase {

    private final ProductFinderService productFinderService;
    private final RedisRankingService redisRankingService;
    private final ProductRedisCacheService productRedisCacheService;
    private final CategoryFinderService categoryFinderService;

    public List<Product> execute(String type, int limit) {

        // 1. Redis에서 인기상품 ID 목록 가져오기
        List<Long> productIds;
        if ("daily".equals(type)) {
            productIds = redisRankingService.getTodayTopProducts(limit);
        } else { // 주간 인기 상품
            productIds = redisRankingService.getWeeklyTopProducts(limit);
        }

        // 2. 빈 목록 체크
        if (productIds.isEmpty()) {
            return List.of();
        }

        // 3. 캐시에서 상품 정보 조회 + 캐시 미스 ID 분리
        List<Product> cachedProducts = new ArrayList<>();
        List<Long> missedIds = new ArrayList<>();

        for (Long productId : productIds) {
            Optional<ProductRedisCacheService.ProductCacheDto> cacheDto;

            if ("daily".equals(type)) {
                cacheDto = productRedisCacheService.getDailyProduct(productId);
            } else {
                cacheDto = productRedisCacheService.getWeeklyProduct(productId);
            }

            if (cacheDto.isPresent()) {
                // 캐시 히트: DTO → Product 변환
                cachedProducts.add(toProduct(cacheDto.get()));
            } else {
                // 캐시 미스: DB 조회 대상
                missedIds.add(productId);
            }
        }

        // 4. 캐시 미스된 것만 DB 조회
        List<Product> dbProducts = List.of();
        if (!missedIds.isEmpty()) {
            dbProducts = productFinderService.getAllProductsById(missedIds);

            // DB 조회한 것들 캐싱
            for (Product product : dbProducts) {
                if ("daily".equals(type)) {
                    productRedisCacheService.cacheDailyProduct(product);
                } else {
                    productRedisCacheService.cacheWeeklyProduct(product);
                }
            }
        }

        // 5. 캐시 + DB 결과 합치기
        List<Product> allProducts = new ArrayList<>(cachedProducts);
        allProducts.addAll(dbProducts);

        // 6. Redis 랭킹 순서대로 정렬
        Map<Long, Product> productMap = allProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        return productIds.stream()
                .map(productMap::get)
                .filter(product -> product != null)  // 삭제된 상품 제외
                .toList();
    }

    /**
     * ProductCacheDto → Product 변환
     * Category는 DB에서 조회 후 Product.fromCache 정적 팩토리 메서드 사용
     */
    private Product toProduct(ProductRedisCacheService.ProductCacheDto dto) {
        // Category를 DB에서 조회
        Category category = categoryFinderService.getActiveCategory(dto.categoryId());

        // Product.fromCache 정적 팩토리 메서드로 생성
        return Product.fromCache(
            dto.id(),
            category,
            dto.name(),
            dto.description(),
            new BigDecimal(dto.price()),
            dto.stock(),
            dto.isActive(),
            dto.viewCount(),
            dto.soldCount(),
            dto.minOrderQuantity(),
            dto.maxOrderQuantity()
        );
    }
}
