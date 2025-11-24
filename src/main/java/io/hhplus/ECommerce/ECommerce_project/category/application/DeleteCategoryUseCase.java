package io.hhplus.ECommerce.ECommerce_project.category.application;

import io.hhplus.ECommerce.ECommerce_project.category.application.service.CategoryFinderService;
import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.domain.service.CategoryDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteCategoryUseCase {

    private final CategoryFinderService finderService;
    private final CategoryDomainService domainService;

    @Transactional
    public void execute(Long id) {

        // 1. ID 값 검증 (Domain Layer)
        domainService.validateId(id);

        // 2. 조회 (Application Layer)
        Category category = finderService.getActiveCategory(id);

        // 3. 삭제 가능 여부 검증 (Domain Layer) - 추가됨
        domainService.validateDeletable(category);

        // 3. 논리적 삭제 (Entity 내부에서 삭제 가능 여부 검증)
        category.delete();
    }
}
