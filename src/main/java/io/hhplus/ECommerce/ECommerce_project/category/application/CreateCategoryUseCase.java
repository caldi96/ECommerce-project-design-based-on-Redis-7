package io.hhplus.ECommerce.ECommerce_project.category.application;

import io.hhplus.ECommerce.ECommerce_project.category.application.command.CreateCategoryCommand;
import io.hhplus.ECommerce.ECommerce_project.category.application.service.CategoryFinderService;
import io.hhplus.ECommerce.ECommerce_project.category.application.service.CategoryValidatorService;
import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.domain.service.CategoryDomainService;
import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final CategoryValidatorService appValidatorService;
    private final CategoryDomainService domainService;
    private final CategoryFinderService finderService;

    @Transactional
    public Category execute(CreateCategoryCommand command) {

        // 1. 순수 검증(domain)
        domainService.validateNameNotEmpty(command.name());

        // 2. DB 기반 중복 검증 (Application Layer)
        appValidatorService.validateNameNotDuplicated(command.name());
        appValidatorService.validateDisplayOrderNotDuplicated(command.displayOrder());

        // 3. 도메인 생성 (Entity 내부에서 순수 검증 수행)
        Category category = Category.createCategory(
                command.name(),
                command.displayOrder()
        );

        // 4. 저장 후 반환
        return categoryRepository.save(category);
    }
}
