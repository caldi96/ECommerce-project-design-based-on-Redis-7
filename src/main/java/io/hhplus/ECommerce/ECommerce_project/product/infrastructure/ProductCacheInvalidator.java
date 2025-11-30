package io.hhplus.ECommerce.ECommerce_project.product.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.product.application.enums.ProductSortType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductCacheInvalidator {

    private static final int FIRST_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final CacheManager redisCacheManager;

    public void evictProductListCache(Long categoryId) {
        Cache cache = redisCacheManager.getCache("productList");
        if (cache != null) {
            // 5가지 정렬 × 첫페이지 캐시 삭제
            for (ProductSortType sortType : ProductSortType.values()) {
                String key = categoryId + "_" + sortType + "_" + FIRST_PAGE + "_" + DEFAULT_PAGE_SIZE;
                cache.evict(key);
            }
        }
    }
}
