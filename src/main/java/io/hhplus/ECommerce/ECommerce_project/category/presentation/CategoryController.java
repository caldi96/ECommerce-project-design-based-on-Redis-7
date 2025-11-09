package io.hhplus.ECommerce.ECommerce_project.category.presentation;

import io.hhplus.ECommerce.ECommerce_project.category.application.*;
import io.hhplus.ECommerce.ECommerce_project.category.presentation.request.CreateCategoryRequest;
import io.hhplus.ECommerce.ECommerce_project.category.presentation.request.UpdateCategoryRequest;
import io.hhplus.ECommerce.ECommerce_project.category.presentation.response.CategoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final GetCategoryListUseCase getCategoryListUseCase;
    private final GetCategoryUseCase getCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    /**
     * 카테고리 등록
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        var createdCategory = createCategoryUseCase.execute(request.toCommand());
        return ResponseEntity.ok(CategoryResponse.from(createdCategory));
    }

    /**
     * 카테고리 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategoryList() {
        var categoryList = getCategoryListUseCase.execute();
        return ResponseEntity.ok(CategoryResponse.from(categoryList));
    }

    /**
     * 카테고리 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable Long id) {
        var category = getCategoryUseCase.execute(id);
        return ResponseEntity.ok(CategoryResponse.from(category));
    }

    /**
     * 카테고리 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        var command = request.toCommand(id);
        var updated = updateCategoryUseCase.execute(command);
        return ResponseEntity.ok(CategoryResponse.from(updated));
    }

    /**
     * 카테고리 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        deleteCategoryUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
