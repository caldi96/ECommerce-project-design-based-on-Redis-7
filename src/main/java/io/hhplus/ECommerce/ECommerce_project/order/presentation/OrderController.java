package io.hhplus.ECommerce.ECommerce_project.order.presentation;

import io.hhplus.ECommerce.ECommerce_project.order.application.CancelOrderUseCase;
import io.hhplus.ECommerce.ECommerce_project.order.application.CreateOrderFromCartUseCase;
import io.hhplus.ECommerce.ECommerce_project.order.application.CreateOrderFromProductUseCase;
import io.hhplus.ECommerce.ECommerce_project.order.application.GetOrderDetailUseCase;
import io.hhplus.ECommerce.ECommerce_project.order.application.GetOrderListUseCase;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CancelOrderCommand;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.request.CreateOrderFromCartRequest;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.request.CreateOrderFromProductRequest;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.request.GetOrderDetailRequest;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.request.GetOrderListRequest;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderFromCartResponse;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.GetOrderDetailResponse;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.GetOrderListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderFromCartUseCase createOrderFromCartUseCase;
    private final CreateOrderFromProductUseCase createOrderFromProductUseCase;
    private final GetOrderListUseCase getOrderListUseCase;
    private final GetOrderDetailUseCase getOrderDetailUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;

    /**
     * 장바구니에서 주문 생성
     */
    @PostMapping("/from-cart")
    public ResponseEntity<CreateOrderFromCartResponse> createOrderFromCart(
            @Valid @RequestBody CreateOrderFromCartRequest request
    ) {
        CreateOrderFromCartResponse response = createOrderFromCartUseCase.execute(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 상품 페이지에서 직접 주문 생성
     */
    @PostMapping("/from-product")
    public ResponseEntity<CreateOrderFromCartResponse> createOrderFromProduct(
            @Valid @RequestBody CreateOrderFromProductRequest request
    ) {
        CreateOrderFromCartResponse response = createOrderFromProductUseCase.execute(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 사용자별 주문 목록 조회 (페이징, 상태 필터링)
     */
    @GetMapping
    public ResponseEntity<GetOrderListResponse> getOrderList(
            @RequestParam Long userId,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        GetOrderListRequest request = new GetOrderListRequest(
                userId,
                orderStatus != null ? io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus.valueOf(orderStatus) : null,
                page,
                size
        );
        GetOrderListResponse response = getOrderListUseCase.execute(request.toCommand());
        return ResponseEntity.ok(response);
    }

    /**
     * 주문 상세 조회
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<GetOrderDetailResponse> getOrderDetail(
            @PathVariable Long orderId,
            @RequestParam Long userId
    ) {
        GetOrderDetailRequest request = new GetOrderDetailRequest(orderId, userId);
        GetOrderDetailResponse response = getOrderDetailUseCase.execute(request.toCommand());
        return ResponseEntity.ok(response);
    }

    /**
     * 주문 취소
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam Long userId
    ) {
        CancelOrderCommand command = new CancelOrderCommand(userId, orderId);
        cancelOrderUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }
}
