package io.hhplus.ECommerce.ECommerce_project.order.application.service;

import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 만료 처리 서비스
 * - 각 주문마다 독립적인 트랜잭션으로 처리
 * - 하나의 주문 실패가 다른 주문에 영향을 주지 않음
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExpirationService {

    private final OrderFinderService orderFinderService;
    private final CompensationService compensationService;

    /**
     * 개별 주문 취소 - 독립적인 트랜잭션
     *
     * @param orderId 취소할 주문 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelOrder(Long orderId) {
        // 1. 비관적 락으로 주문 조회 (동시성 제어)
        Orders order = orderFinderService.getOrderWithLock(orderId);

        // 2. 재확인: PENDING 상태인지 체크
        //    (스케줄러 조회 이후 다른 곳에서 결제 완료될 수 있음)
        if (!order.isPending()) {
            log.debug("주문이 이미 처리됨: orderId={}, status={}", orderId, order.getStatus());
            return;
        }

        // 3. 보상 트랜잭션 실행 (재고/쿠폰/포인트 복구)
        compensationService.compensate(order);

        // 4. 주문 상태 변경: PENDING → CANCELED
        order.cancel();

        log.info("주문 자동 취소 완료: orderId={}, createdAt={}",
                orderId, order.getCreatedAt());
    }
}