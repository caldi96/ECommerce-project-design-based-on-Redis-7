package io.hhplus.ECommerce.ECommerce_project.category.domain.repository;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(Long id);

    List<Category> findAll();

    boolean existsByCategoryName(String name);

    boolean existsByDisplayOrder(int displayOrder);

    boolean existsByCategoryNameExceptId(String name, Long excludedId);

    boolean existsByDisplayOrderExceptId(int displayOrder, Long excludedId);

    void deleteById(Long id);
}
