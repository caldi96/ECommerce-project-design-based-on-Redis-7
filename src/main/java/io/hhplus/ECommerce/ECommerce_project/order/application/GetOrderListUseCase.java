package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.GetOrderListCommand;
import io.hhplus.ECommerce.ECommerce_project.order.application.dto.OrdersPageResult;
import io.hhplus.ECommerce.ECommerce_project.order.application.service.OrderFinderService;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetOrderListUseCase {

    private final UserDomainService userDomainService;
    private final UserFinderService userFinderService;
    private final OrderFinderService orderFinderService;

    @Transactional(readOnly = true)
    public OrdersPageResult execute(GetOrderListCommand command) {

        // 1. Id 검증
        userDomainService.validateId(command.userId());

        // 2. 사용자 존재 확인
        User user = userFinderService.getUser(command.userId());

        // 3. Pageable 생성
        Pageable pageable = PageRequest.of(command.page(), command.size());

        // 4. 주문 목록 조회 (페이징, 필터링)
        Page<Orders> ordersPage = orderFinderService.getOrdersPageByUserId(
                command.userId(),
                command.orderStatus(),
                pageable
        );

        // 5. Response 생성
        return new OrdersPageResult(
                ordersPage.getContent(),
                ordersPage.getNumber(),
                ordersPage.getSize(),
                ordersPage.getTotalElements(),
                ordersPage.getTotalPages(),
                ordersPage.isFirst(),
                ordersPage.isLast()
        );
    }
}