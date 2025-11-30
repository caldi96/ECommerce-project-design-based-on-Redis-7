package io.hhplus.ECommerce.ECommerce_project.order.application.scheduler;

import io.hhplus.ECommerce.ECommerce_project.order.application.service.OrderExpirationService;
import io.hhplus.ECommerce.ECommerce_project.order.application.service.OrderFinderService;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 만료 스케줄러
 * - 매 1분마다 15분 이상 PENDING 상태인 주문을 자동 취소
 * - 각 주문은 독립적인 트랜잭션으로 처리 (한 주문 실패가 다른 주문에 영향 없음)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrderFinderService orderFinderService;
    private final OrderExpirationService orderExpirationService;

    /**
     * 이전 작업 완료 후 1분 대기하여 15분 이상 PENDING 상태인 주문 취소 - 주문 만료 동작이 1분 안에 다 되지 않은 상태에서 될 수 있으므로 fixedRate -> fixedDelay로 변경
     */
    @Scheduled(fixedDelay = 60000) // 이전 작업 완료 후 1분 대기
    public void cancelExpiredOrders() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(15);

        // PENDING 상태이면서 15분 이상 지난 주문 조회
        List<Orders> expiredOrders = orderFinderService.getExpiredOrders(OrderStatus.PENDING, expirationTime);

        if (expiredOrders.isEmpty()) return;

        log.info("만료된 주문 {}건 취소 처리 시작", expiredOrders.size());

        int successCount = 0;
        int failCount = 0;

        for (Orders order : expiredOrders) {
            try {
                // 각 주문마다 독립적인 트랜잭션으로 처리
                orderExpirationService.cancelOrder(order.getId());
                successCount++;
            } catch (Exception e) {
                log.error("주문 자동 취소 실패: orderId={}, error={}", order.getId(), e.getMessage(), e);
                failCount++;
                // 실패한 주문은 다음 스케줄에서 재시도됨
            }
        }

        log.info("만료된 주문 취소 처리 완료: 성공={}, 실패={}", successCount, failCount);
    }
}
