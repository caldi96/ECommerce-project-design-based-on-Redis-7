package io.hhplus.ECommerce.ECommerce_project.category.application.command;

public record CreateCategoryCommand(
        String name,
        int displayOrder
) {}
