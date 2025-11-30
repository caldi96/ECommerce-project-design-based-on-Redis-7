package io.hhplus.ECommerce.ECommerce_project.product.application.warmer;

import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.RedisStockService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 재고 캐시 워머
 * - 애플리케이션 시작 시 DB → Redis 재고 동기화
 * - 초기 캐시 워밍으로 일관성 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockCacheWarmer implements ApplicationRunner {

    private final ProductFinderService productFinderService;
    private final RedisStockService redisStockService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== 재고 캐시 워밍 시작 ===");

        try {
            // DB → Redis 재고 동기화
            List<Product> products = productFinderService.getAllProduct();

            int successCount = 0;
            int failCount = 0;

            for (Product product : products) {
                try {
                    // Redis에 재고 설정
                    redisStockService.setStock(product.getId(), product.getStock());
                    successCount++;

                    log.debug("재고 캐시 설정 완료: productId={}, productName={}, stock={}",
                            product.getId(), product.getName(), product.getStock());

                } catch (Exception e) {
                    failCount++;
                    log.error("재고 캐시 설정 실패: productId={}, productName={}",
                            product.getId(), product.getName(), e);
                }
            }

            log.info("=== 재고 캐시 워밍 완료 === 성공: {}, 실패: {}, 전체: {}",
                    successCount, failCount, products.size());

        } catch (Exception e) {
            log.error("재고 캐시 워밍 실패", e);
            // 실패해도 애플리케이션은 계속 실행 (DB 비관적 락으로 폴백 가능)
        }
    }
}
