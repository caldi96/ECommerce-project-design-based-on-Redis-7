package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.GetOrderDetailCommand;
import io.hhplus.ECommerce.ECommerce_project.order.application.service.OrderFinderService;
import io.hhplus.ECommerce.ECommerce_project.order.application.service.OrderItemFinderService;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.service.OrderDomainService;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.GetOrderDetailResponse;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrderDetailUseCase {

    private final UserDomainService userDomainService;
    private final OrderDomainService orderDomainService;
    private final UserFinderService userFinderService;
    private final OrderFinderService orderFinderService;
    private final OrderItemFinderService orderItemFinderService;

    @Transactional(readOnly = true)
    public GetOrderDetailResponse execute(GetOrderDetailCommand command) {

        // 1. ID 검증
        orderDomainService.validateId(command.orderId());
        userDomainService.validateId(command.userId());

        // 2. 사용자 존재 확인
        User user = userFinderService.getUser(command.userId());

        // 3. 주문 조회
        Orders order = orderFinderService.getOrder(command.orderId());

        // 4. 주문 소유자 확인 (권한 체크)
        orderDomainService.validateOrderOwner(order, user);

        // 5. 주문 항목 조회 후 Response 생성
        List<OrderItem> orderItems = orderItemFinderService.getOrderItems(command.orderId());

        return GetOrderDetailResponse.of(order, orderItems);
    }
}
