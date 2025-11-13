package io.hhplus.ECommerce.ECommerce_project.category.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.domain.repository.CategoryMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
public class CategoryMemoryRepositoryImpl implements CategoryMemoryRepository {
    private final Map<Long, Category> categoryMap = new ConcurrentHashMap<>();
    private final SnowflakeIdGenerator idGenerator;

    @Override
    public Category save(Category category) {
        // ID가 없으면 Snowflake ID 생성
        if (category.getId() == null) {
            category.setId(idGenerator.nextId());
        }
        categoryMap.put(category.getId(), category);
        return category;
    }

    @Override
    public Optional<Category> findById(Long id) {
        return Optional.ofNullable(categoryMap.get(id))
                .filter(category -> !category.isDeleted());
    }

    @Override
    public List<Category> findAll() {
        return categoryMap.values().stream()
                .filter(category -> !category.isDeleted())
                .toList();
    }

    @Override
    public boolean existsByCategoryName(String name) {
        return categoryMap.values().stream()
                .filter(category -> !category.isDeleted())
                .anyMatch(category -> category.getCategoryName().equals(name));
    }

    @Override
    public boolean existsByDisplayOrder(int displayOrder) {
        return categoryMap.values().stream()
                .filter(category -> !category.isDeleted())
                .anyMatch(category -> category.getDisplayOrder() == displayOrder);
    }

    @Override
    public boolean existsByCategoryNameExceptId(String name, Long excludedId) {
        return categoryMap.values().stream()
                .filter(category -> !category.isDeleted())
                .anyMatch(category -> !category.getId().equals(excludedId) &&
                                                category.getCategoryName().equals(name));
    }

    @Override
    public boolean existsByDisplayOrderExceptId(int displayOrder, Long excludedId) {
        return categoryMap.values().stream()
                .filter(category -> !category.isDeleted())
                .anyMatch(category -> !category.getId().equals(excludedId) &&
                                                category.getDisplayOrder() == displayOrder);
    }

    @Override
    public void deleteById(Long id) {
        categoryMap.remove(id);
    }
}
