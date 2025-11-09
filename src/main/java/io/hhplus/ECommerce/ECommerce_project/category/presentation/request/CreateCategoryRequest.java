package io.hhplus.ECommerce.ECommerce_project.category.presentation.request;

import io.hhplus.ECommerce.ECommerce_project.category.application.command.CreateCategoryCommand;
import jakarta.validation.constraints.*;


public record CreateCategoryRequest(

        @NotBlank(message = "카테고리 이름은 필수입니다")
        String name,

        @NotNull(message = "표시 순서는 필수입니다")
        @Min(value = 1, message = "표시 순서는 1 이상이어야 합니다")
        Integer displayOrder
) {

    public CreateCategoryCommand toCommand() {
        return new CreateCategoryCommand(
                name,
                displayOrder
        );
    }
}
