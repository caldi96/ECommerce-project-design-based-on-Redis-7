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
    private final SnowflakeIdGenerator idGenerator;

    @Override
    public Orders save(Orders orders) {
        // ID가 없으면 Snowflake ID 생성
        if (orders.getId() == null) {
            orders.setId(idGenerator.nextId());
        }
        orderMap.put(orders.getId(), orders);
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
}
