package io.hhplus.ECommerce.ECommerce_project.order.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.common.SnowflakeIdGenerator;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderMemoryRepository implements OrderRepository {
    private final Map<Long, Orders> orderMap = new ConcurrentHashMap<>();
    private final Map<Long, Object> lockMap = new ConcurrentHashMap<>();  // 주문별 락 객체
    private final SnowflakeIdGenerator idGenerator;

    /**
     * 주문 ID별 락 객체 획득
     */
    private Object getLock(Long orderId) {
        return lockMap.computeIfAbsent(orderId, k -> new Object());
    }

    @Override
    public Orders save(Orders orders) {
        // ID가 없으면 Snowflake ID 생성
        if (orders.getId() == null) {
            orders.setId(idGenerator.nextId());
        }

        // 주문 ID가 있는 경우 락을 걸고 저장 (동시성 제어)
        if (orders.getId() != null) {
            Object lock = lockMap.computeIfAbsent(orders.getId(), k -> new Object());
            synchronized (lock) {
                orderMap.put(orders.getId(), orders);
            }
        } else {
            orderMap.put(orders.getId(), orders);
        }

        return orders;
    }

    @Override
    public Optional<Orders> findById(Long id) {
        return Optional.ofNullable(orderMap.get(id));
    }

    @Override
    public List<Orders> findAll() {
        return new ArrayList<>(orderMap.values());
    }

    @Override
    public void deletedById(Long id) {
        orderMap.remove(id);
    }

    @Override
    public List<Orders> findByUserId(Long userId, OrderStatus orderStatus, int page, int size) {
        return orderMap.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .filter(order -> orderStatus == null || order.getStatus() == orderStatus)
                .sorted(Comparator.comparing(Orders::getCreatedAt).reversed()) // 최신순
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(Long userId, OrderStatus orderStatus) {
        return orderMap.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .filter(order -> orderStatus == null || order.getStatus() == orderStatus)
                .count();
    }

    /**
     * 동시성 제어를 위한 주문 조회 (비관적 락)
     * 주문 ID별로 락을 걸어서 Race Condition 방지
     */
    @Override
    public Optional<Orders> findByIdWithLock(Long orderId) {
        Object lock = getLock(orderId);

        synchronized (lock) {
            return Optional.ofNullable(orderMap.get(orderId));
        }
    }
}
