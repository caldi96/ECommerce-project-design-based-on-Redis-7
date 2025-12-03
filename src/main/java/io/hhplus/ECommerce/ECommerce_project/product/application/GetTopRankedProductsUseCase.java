package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.RedisRankingService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class GetTopRankedProductsUseCase {

    private final ProductFinderService productFinderService;
    private final RedisRankingService redisRankingService;

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

        // 3. DB에서 상품 정보 일괄 조회
        List<Product> products = productFinderService.getAllProductsById(productIds);

        // 4. Redis 순서대로 정렬 + 존재하는 상품만 필터링
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<Product> sortedProducts = productIds.stream()
                .map(productMap::get)
                .filter(product -> product != null)  // 삭제된 상품 제외
                .toList();


        return sortedProducts;
    }
}
