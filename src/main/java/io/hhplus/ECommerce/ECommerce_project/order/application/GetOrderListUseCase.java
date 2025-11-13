package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.GetOrderListCommand;
import io.hhplus.ECommerce.ECommerce_project.order.application.dto.OrdersPageResult;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetOrderListUseCase {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrdersPageResult execute(GetOrderListCommand command) {
        // 1. 사용자 존재 확인
        userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. Pageable 생성
        Pageable pageable = PageRequest.of(command.page(), command.size());

        // 3. 주문 목록 조회 (페이징, 필터링)
        Page<Orders> ordersPage = orderRepository.findByUserIdWithPaging(
                command.userId(),
                command.orderStatus(),
                pageable
        );

        /*
        // 4. 전체 주문 개수 조회
        long totalElements = orderRepository.countByUserId(
                command.userId(),
                command.orderStatus()
        );
         */

        // 4. Response 생성
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