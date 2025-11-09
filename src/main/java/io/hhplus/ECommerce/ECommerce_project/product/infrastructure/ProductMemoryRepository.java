package io.hhplus.ECommerce.ECommerce_project.product.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.common.SnowflakeIdGenerator;
import io.hhplus.ECommerce.ECommerce_project.product.application.enums.ProductSortType;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProductMemoryRepository implements ProductRepository {
    private final Map<Long, Product> productMap = new ConcurrentHashMap<>();
    private final Map<Long, Object> lockMap = new ConcurrentHashMap<>(); // 상품별 락 객체
    private final SnowflakeIdGenerator idGenerator;

    @Override
    public Product save(Product product) {
        // ID가 없으면 Snowflake ID 생성
        if (product.getId() == null) {
            product.setId(idGenerator.nextId());
        }

        // 상품 ID가 있는 경우 락을 걸고 저장 (동시성 제어)
        if (product.getId() != null) {
            Object lock = lockMap.computeIfAbsent(product.getId(), k -> new Object());
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
        // 상품별 락 객체 획득 (없으면 생성)
        Object lock = lockMap.computeIfAbsent(id, k -> new Object());

        // 해당 상품에 대한 락을 획득하고 조회
        synchronized (lock) {
            return Optional.ofNullable(productMap.get(id))
                    .filter(p -> p.getDeletedAt() == null);
        }
    }

    /**
     * 재고 복구 (비관적 락 적용)
     * 락 안에서 재고 증가 및 판매량 감소를 원자적으로 수행하여 동시성 문제 해결
     */
    @Override
    public Product restoreStockWithLock(Long productId, int quantity) {
        // 상품별 락 객체 획득
        Object lock = lockMap.computeIfAbsent(productId, k -> new Object());

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
        // 1. 활성화되고 삭제되지 않은 상품만 필터링
        var stream = productMap.values().stream()
                .filter(Product::isActive)
                .filter(p -> p.getDeletedAt() == null);

        // 2. 카테고리 필터링 (카테고리 ID가 있는 경우)
        if (categoryId != null) {
            stream = stream.filter(p -> categoryId.equals(p.getCategoryId()));
        }

        // 3. 정렬
        Comparator<Product> comparator = getComparator(sortType);
        stream = stream.sorted(comparator);

        // 4. 페이징 (skip & limit)
        return stream
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public long countActiveProducts(Long categoryId) {
        var stream = productMap.values().stream()
                .filter(Product::isActive)
                .filter(p -> p.getDeletedAt() == null);

        if (categoryId != null) {
            stream = stream.filter(p -> categoryId.equals(p.getCategoryId()));
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
