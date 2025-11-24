package io.hhplus.ECommerce.ECommerce_project.category.application;

import io.hhplus.ECommerce.ECommerce_project.category.application.service.CategoryFinderService;
import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.domain.service.CategoryDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCategoryUseCase {

    private final CategoryFinderService finderService;
    private final CategoryDomainService domainService;

    @Transactional(readOnly = true)
    public Category execute(Long id) {

        // 1. ID 값 검증 (Domain Layer)
        domainService.validateId(id);

        // 2. category 조회 밑 반환 (Application Layer)
        return finderService.getActiveCategory(id);
    }
}
