package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.OrderException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.GetOrderDetailCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.GetOrderDetailResponse;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrderDetailUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public GetOrderDetailResponse execute(GetOrderDetailCommand command) {
        // 1. 사용자 존재 확인
        userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 주문 조회
        Orders order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

        // 3. 주문 소유자 확인 (권한 체크)
        if (!order.getUserId().equals(command.userId())) {
            throw new OrderException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // 4. 주문 항목 조회 후 Response 생성
        List<OrderItem> orderItemList = orderItemRepository.findByOrderId(command.orderId());

        return GetOrderDetailResponse.of(order, orderItemList);
    }
}
