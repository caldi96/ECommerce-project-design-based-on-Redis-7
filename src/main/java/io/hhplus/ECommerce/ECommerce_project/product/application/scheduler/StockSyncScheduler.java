package io.hhplus.ECommerce.ECommerce_project.product.application.scheduler;

import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.RedisStockService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 재고 동기화 스케줄러
 * - 주기적으로 DB와 Redis 재고 정합성 검증
 * - Eventual Consistency 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncScheduler {

    private final ProductFinderService productFinderService;
    private final RedisStockService redisStockService;

    /**
     * DB-Redis 재고 정합성 검증
     * - 1분마다 실행
     * - 불일치 발견 시 DB를 정답으로 Redis 복구
     */
    @Scheduled(fixedDelay = 60000) // 60초 = 1분
    public void validateStockConsistency() {
        log.debug("=== 재고 정합성 검증 시작 ===");

        try {
            List<Product> products = productFinderService.getAllProduct();

            int totalCount = 0;
            int mismatchCount = 0;

            for (Product product : products) {
                totalCount++;

                try {
                    // Redis 재고 조회
                    Long redisStock = redisStockService.getStock(product.getId());

                    // DB 재고와 비교
                    if (product.getStock() != redisStock.intValue()) {
                        mismatchCount++;

                        log.warn("재고 불일치 감지: productId={}, productName={}, DB={}, Redis={}",
                                product.getId(), product.getName(), product.getStock(), redisStock);

                        // DB를 정답으로 Redis 복구
                        redisStockService.setStock(product.getId(), product.getStock());

                        log.info("재고 복구 완료: productId={}, 복구된 재고={}",
                                product.getId(), product.getStock());
                    }

                } catch (Exception e) {
                    log.error("재고 검증 실패: productId={}, productName={}",
                            product.getId(), product.getName(), e);
                }
            }

            if (mismatchCount > 0) {
                log.warn("=== 재고 정합성 검증 완료 === 전체: {}, 불일치: {}, 복구 완료",
                        totalCount, mismatchCount);
            } else {
                log.debug("=== 재고 정합성 검증 완료 === 전체: {}, 모두 일치",
                        totalCount);
            }

        } catch (Exception e) {
            log.error("재고 정합성 검증 실패", e);
        }
    }

    /**
     * Redis에 없는 상품 재고 초기화
     * - 5분마다 실행
     * - 새로 추가된 상품이 Redis에 없는 경우 대비
     */
    @Scheduled(fixedDelay = 300000) // 300초 = 5분
    public void initializeMissingStock() {
        log.debug("=== 누락된 재고 초기화 시작 ===");

        try {
            List<Product> products = productFinderService.getAllProduct();

            int initializedCount = 0;

            for (Product product : products) {
                try {
                    // Redis에 재고 존재 여부 확인
                    if (!redisStockService.existsStock(product.getId())) {
                        // 없으면 DB 재고로 초기화
                        redisStockService.setStock(product.getId(), product.getStock());
                        initializedCount++;

                        log.info("누락된 재고 초기화: productId={}, productName={}, stock={}",
                                product.getId(), product.getName(), product.getStock());
                    }

                } catch (Exception e) {
                    log.error("재고 초기화 실패: productId={}, productName={}",
                            product.getId(), product.getName(), e);
                }
            }

            if (initializedCount > 0) {
                log.info("=== 누락된 재고 초기화 완료 === 초기화된 상품: {}", initializedCount);
            } else {
                log.debug("=== 누락된 재고 초기화 완료 === 누락된 상품 없음");
            }

        } catch (Exception e) {
            log.error("누락된 재고 초기화 실패", e);
        }
    }
}
