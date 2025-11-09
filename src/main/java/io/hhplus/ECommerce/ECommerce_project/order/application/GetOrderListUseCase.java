package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.GetOrderListCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.GetOrderListResponse;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrderListUseCase {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public GetOrderListResponse execute(GetOrderListCommand command) {
        // 1. 사용자 존재 확인
        userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 주문 목록 조회 (페이징, 필터링)
        List<Orders> orders = orderRepository.findByUserId(
                command.userId(),
                command.orderStatus(),
                command.page(),
                command.size()
        );

        // 3. 전체 주문 개수 조회
        long totalElements = orderRepository.countByUserId(
                command.userId(),
                command.orderStatus()
        );

        // 4. Response 생성
        return GetOrderListResponse.of(
                orders,
                command.page(),
                command.size(),
                totalElements
        );
    }
}