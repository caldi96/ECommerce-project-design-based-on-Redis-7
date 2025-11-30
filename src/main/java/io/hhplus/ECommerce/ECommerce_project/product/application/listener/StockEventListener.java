package io.hhplus.ECommerce.ECommerce_project.product.application.listener;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.event.StockDecreasedEvent;
import io.hhplus.ECommerce.ECommerce_project.product.domain.event.StockIncreasedEvent;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 재고 이벤트 리스너
 * - Redis 재고 변경 후 DB 동기화를 비동기로 처리
 * - Eventual Consistency 패턴 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventListener {

    private final ProductRepository productRepository;

    /**
     * 재고 차감 이벤트 처리
     * - Redis에서 재고 차감 후 DB에 반영
     * - 비동기로 처리되어 사용자 응답 속도에 영향 없음
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStockDecreased(StockDecreasedEvent event) {
        try {
            log.debug("재고 차감 이벤트 처리 시작: productId={}, quantity={}",
                    event.productId(), event.quantity());

            // 비관적 락으로 상품 조회 (DB 동시성 제어)
            Product product = productRepository.findByIdWithLock(event.productId())
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

            // DB 재고 차감 및 판매량 증가
            product.decreaseStock(event.quantity());
            product.increaseSoldCount(event.quantity());

            log.info("DB 재고 차감 완료: productId={}, quantity={}, remainingStock={}",
                    event.productId(), event.quantity(), product.getStock());

        } catch (Exception e) {
            // 실패 시 로깅 및 알림 (재시도 로직은 필요에 따라 추가)
            log.error("DB 재고 차감 실패: productId={}, quantity={}",
                    event.productId(), event.quantity(), e);

            // TODO: 실패 시 보상 트랜잭션 또는 알림 발송
            // - Redis 재고 복구
            // - 관리자 알림
            // - Dead Letter Queue 저장
        }
    }

    /**
     * 재고 증가 이벤트 처리
     * - 보상 트랜잭션으로 재고 복구 시 DB 반영
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStockIncreased(StockIncreasedEvent event) {
        try {
            log.debug("재고 증가 이벤트 처리 시작: productId={}, quantity={}",
                    event.productId(), event.quantity());

            // 비관적 락으로 상품 조회
            Product product = productRepository.findByIdWithLock(event.productId())
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

            // DB 재고 증가 및 판매량 감소
            product.increaseStock(event.quantity());
            product.decreaseSoldCount(event.quantity());

            log.info("DB 재고 증가 완료: productId={}, quantity={}, remainingStock={}",
                    event.productId(), event.quantity(), product.getStock());

        } catch (Exception e) {
            log.error("DB 재고 증가 실패: productId={}, quantity={}",
                    event.productId(), event.quantity(), e);

            // TODO: 실패 시 처리 로직
        }
    }
}