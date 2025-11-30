package io.hhplus.ECommerce.ECommerce_project.category.application;

import io.hhplus.ECommerce.ECommerce_project.category.application.service.CategoryFinderService;
import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCategoryListUseCase {

    private final CategoryFinderService finderService;

    @Cacheable(value = "categoryList", cacheManager = "localCacheManager")
    @Transactional(readOnly = true)
    public List<Category> execute() {
        // 조회는 FinderService에 위임
        return finderService.getAllActiveCategories();
    }
}
