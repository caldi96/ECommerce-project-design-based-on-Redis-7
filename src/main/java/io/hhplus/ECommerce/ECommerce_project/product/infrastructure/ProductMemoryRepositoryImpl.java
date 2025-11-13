package io.hhplus.ECommerce.ECommerce_project.product.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.common.SnowflakeIdGenerator;
import io.hhplus.ECommerce.ECommerce_project.product.application.enums.ProductSortType;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.OrderException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
public class ProductMemoryRepositoryImpl implements ProductMemoryRepository {
    private final Map<Long, Product> productMap = new ConcurrentHashMap<>();
    private final Map<Long, Object> lockMap = new ConcurrentHashMap<>(); // 상품별 락 객체
    private final SnowflakeIdGenerator idGenerator;

    /**
     * 상품 ID별 락 객체 획득
     */
    private Object getLock(Long productId) {
        return lockMap.computeIfAbsent(productId, k -> new Object());
    }

    @Override
    public Product save(Product product) {
        // ID가 없으면 Snowflake ID 생성
        if (product.getId() == null) {
            product.setId(idGenerator.nextId());
        }

        // 상품 ID가 있는 경우 락을 걸고 저장 (동시성 제어)
        if (product.getId() != null) {
            Object lock = getLock(product.getId());
            synchronized (lock) {
                productMap.put(product.getId(), product);
            }
        } else {
            productMap.put(product.getId(), product);
        }

        return product;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(productMap.get(id))
                .filter(p -> p.getDeletedAt() == null);  // 삭제되지 않은 상품만 반환
    }

    @Override
    public Optional<Product> findByIdWithLock(Long id) {
        Object lock = getLock(id);

        synchronized (lock) {
            return Optional.ofNullable(productMap.get(id))
                    .filter(p -> p.getDeletedAt() == null);
        }
    }

    /**
     * 재고 차감 (비관적 락 적용)
     * 락 안에서 상품 조회, 검증, 재고 차감, 판매량 증가를 원자적으로 수행하여 동시성 문제 해결
     */
    @Override
    public Product decreaseStockWithLock(Long productId, int quantity) {
        Object lock = getLock(productId);

        synchronized (lock) {
            Product product = productMap.get(productId);
            if (product != null) {
                // 상품 주문 가능 여부 확인 (활성화, 재고)
                if (!product.canOrder(quantity)) {
                    throw new OrderException(
                            ErrorCode.ORDER_PRODUCT_CANNOT_BE_ORDERED);
                }

                // 재고 차감 및 판매량 증가를 락 안에서 원자적으로 수행
                product.decreaseStock(quantity);
                product.increaseSoldCount(quantity);
                productMap.put(productId, product);
            }
            return product;
        }
    }

    /**
     * 재고 복구 (비관적 락 적용)
     * 락 안에서 재고 증가 및 판매량 감소를 원자적으로 수행하여 동시성 문제 해결
     */
    @Override
    public Product restoreStockWithLock(Long productId, int quantity) {
        Object lock = getLock(productId);

        synchronized (lock) {
            Product product = productMap.get(productId);
            if (product != null) {
                // 재고 증가 및 판매량 감소를 락 안에서 원자적으로 수행
                product.increaseStock(quantity);
                product.decreaseSoldCount(quantity);
                productMap.put(productId, product);
            }
            return product;
        }
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(productMap.values());
    }

    @Override
    public List<Product> findAllById(List<Long> ids) {
        return ids.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .filter(p -> p.getDeletedAt() == null)  // 삭제되지 않은 상품만 반환
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        productMap.remove(id);
    }

    @Override
    public List<Product> findProducts(Long categoryId, ProductSortType sortType, int page, int size) {
        // 1. 활성화되고 삭제되지 않은 상품중 categoryId와 맞는것만 필터링
        var stream = productMap.values().stream()
                .filter(Product::isActive)
                .filter(product -> product.getDeletedAt() == null)
                .filter(product -> categoryId == null || categoryId.equals(product.getCategory().getId()));

        // 3. 정렬
        Comparator<Product> comparator = getComparator(sortType);
        stream = stream.sorted(comparator);

        // 4. 페이징 (skip & limit)
        return stream
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    @Override
    public long countActiveProducts(Long categoryId) {
        var stream = productMap.values().stream()
                .filter(Product::isActive)
                .filter(product -> product.getDeletedAt() == null);

        if (categoryId != null) {
            stream = stream.filter(product -> categoryId.equals(product.getCategory().getId()));
        }

        return stream.count();
    }

    private Comparator<Product> getComparator(ProductSortType sortType) {
        return switch (sortType) {
            case POPULAR -> Comparator.comparing(Product::getSoldCount).reversed();
            case VIEWED -> Comparator.comparing(Product::getViewCount).reversed();
            case PRICE_LOW -> Comparator.comparing(Product::getPrice);
            case PRICE_HIGH -> Comparator.comparing(Product::getPrice).reversed();
            case LATEST -> Comparator.comparing(Product::getCreatedAt).reversed();
        };
    }
}
