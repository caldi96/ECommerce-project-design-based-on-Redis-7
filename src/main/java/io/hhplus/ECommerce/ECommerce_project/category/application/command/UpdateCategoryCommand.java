package io.hhplus.ECommerce.ECommerce_project.category.application.command;

public record UpdateCategoryCommand(
        Long id,
        String name,
        int displayOrder
) {
}
