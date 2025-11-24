package io.hhplus.ECommerce.ECommerce_project.product.application.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;

    // CreateOrderFromProductUseCase.java 에서 사용
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserveStock(Long productId, Integer quantity) {
        // 1. 비관적 락으로 상품 조회
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        // 2. 주문 가능 여부 재검증
        product.validateOrder(quantity);

        // 3. 재고 차감 & 판매량 증가
        product.decreaseStock(quantity);
        product.increaseSoldCount(quantity);

        // 즉시 커밋 -> 락 해제
    }

    // CreateOrderFromProductUseCase.java 에서 사용
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensateStock(Long productId, Integer quantity) {
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        // 재고 복구 & 판매량 감소
        product.increaseStock(quantity);
        product.decreaseSoldCount(quantity);
    }

    // CreateOrderFromCartUseCase,java 에서 사용
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserveStocks(List<Map.Entry<Long, Integer>> sortedEntries) {
        for (Map.Entry<Long, Integer> entry : sortedEntries) {
            // 비관적 락 → 재고 차감
            Long productId = entry.getKey();
            Integer totalQuantity = entry.getValue();

            // 1. 상품 조회 및 검증 (비관적 락 적용 - 원자적 처리)
            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

            // 2. 주문 가능 여부 재검증 (비활성/재고/최소/최대 주문량 체크)
            product.validateOrder(totalQuantity);

            // 3. 재고 차감 및 판매량 증가
            product.decreaseStock(totalQuantity);
            product.increaseSoldCount(totalQuantity);

        }
        // 즉시 커밋 -> 락 해제
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensateStocks(List<Map.Entry<Long, Integer>> sortedEntries) {
        for (Map.Entry<Long, Integer> entry : sortedEntries) {
            // 비관적 락 → 재고 복구
            Long productId = entry.getKey();
            Integer totalQuantity = entry.getValue();

            // 1. 상품 조회 및 검증 (비관적 락 적용 - 원자적 처리)
            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

            // 2. 재고 증가 및 판매량 감소
            product.increaseStock(totalQuantity);
            product.decreaseSoldCount(totalQuantity);
        }
    }
}
