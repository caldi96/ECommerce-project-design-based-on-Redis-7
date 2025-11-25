package io.hhplus.ECommerce.ECommerce_project.category.application;

import io.hhplus.ECommerce.ECommerce_project.category.application.command.UpdateCategoryCommand;
import io.hhplus.ECommerce.ECommerce_project.category.application.service.CategoryFinderService;
import io.hhplus.ECommerce.ECommerce_project.category.application.service.CategoryValidatorService;
import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.domain.service.CategoryDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCategoryUseCase {

    private final CategoryValidatorService appValidatorService;
    private final CategoryFinderService finderService;
    private final CategoryDomainService domainService;

    @CacheEvict(value = "categoryList", allEntries = true)
    @Transactional
    public Category execute(UpdateCategoryCommand command) {

        // 1. ID 값 검증 (Domain Layer)
        domainService.validateId(command.id());

        // 2. 카테고리 조회 (Application Layer)
        Category category = finderService.getActiveCategory(command.id());

        // 3. 유효성 검증 (Domain Layer) - 추가됨
        domainService.validateNameNotEmpty(command.name());
        domainService.validateDisplayOrderPositive(command.displayOrder());

        // 3. DB 기반 중복 검증 (Application Layer)
        appValidatorService.validateNameNotDuplicatedExceptSelf(command.name(), command.id());
        appValidatorService.validateDisplayOrderNotDuplicatedExceptSelf(command.displayOrder(), command.id());

        // 4. 도메인 수정 (Entity 내부에서 순수 검증 수행)
        category.updateCategoryName(command.name());
        category.updateDisplayOrder(command.displayOrder());

        return category;
    }
}
