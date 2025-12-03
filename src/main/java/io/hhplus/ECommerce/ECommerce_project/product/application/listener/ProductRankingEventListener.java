package io.hhplus.ECommerce.ECommerce_project.product.application.listener;

import io.hhplus.ECommerce.ECommerce_project.payment.domain.event.PaymentCompletedEvent;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.RedisRankingService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.event.ProductViewedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 상품 랭킹 이벤트 리스너
 * - 결제 완료 후 Redis 랭킹 업데이트를 비동기로 처리
 * - Eventual Consistency 패턴 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRankingEventListener {

    private final RedisRankingService redisRankingService;

    /**
     * 결제 완료 이벤트 처리
     * - 트랜잭션 커밋 후 Redis 랭킹 업데이트
     * - 비동기로 처리되어 결제 응답 속도에 영향 없음
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            log.info("결제 완료 이벤트 수신 - orderId: {}", event.orderId());

            event.orderItems().forEach(orderItemInfo -> {
                try {
                    redisRankingService.incrementSoldCount(
                            orderItemInfo.productId(),
                            orderItemInfo.quantity()
                    );
                    log.debug("Redis 랭킹 업데이트 완료 - productId: {}, quantity: {}",
                            orderItemInfo.productId(), orderItemInfo.quantity());
                } catch (Exception e) {
                    // 개별 상품 업데이트 실패는 로깅만 (다른 상품은 계속 처리)
                    log.error("Redis 랭킹 업데이트 실패 - productId: {}, quantity: {}",
                            orderItemInfo.productId(), orderItemInfo.quantity(), e);
                }
            });

            log.info("결제 완료 이벤트 처리 완료 - orderId: {}, itemCount: {}",
                    event.orderId(), event.orderItems().size());

        } catch (Exception e) {
            // Redis 업데이트 실패는 로깅만 하고 트랜잭션에 영향 없음
            log.error("결제 완료 이벤트 처리 실패 - orderId: {}", event.orderId(), e);

            // TODO: 실패 시 처리 로직
            // - 재시도 큐에 추가
            // - 관리자 알림
            // - Dead Letter Queue 저장
        }
    }

    /**
     * 상품 조회 이벤트 처리
     * - 트랜잭션 커밋 후 Redis 조회수 증가
     * - 비동기로 처리되어 상품 조회 응답 속도에 영향 없음
     */
    @Async@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductViewed(ProductViewedEvent event) {
        try {
            log.debug("상품 조회 이벤트 수신 - productId: {}", event.productId());

            redisRankingService.incrementViewCount(event.productId());

            log.debug("Redis 조회수 증가 완료 - productId: {}", event.productId());
        } catch (Exception e) {
            // Redis 업데이트 실패는 로깅만 하고 트랜잭션에 영향 없음
            log.error("Redis 조회수 증가 실패 - productId: {}", event.productId(), e);

            // TODO: 실패 시 처리 로직
            // - 재시도 큐에 추가
            // - 모니터링 알림
        }
    }
}
