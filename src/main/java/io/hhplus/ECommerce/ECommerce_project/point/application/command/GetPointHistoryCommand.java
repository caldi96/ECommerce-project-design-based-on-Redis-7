package io.hhplus.ECommerce.ECommerce_project.point.application.command;

public record GetPointHistoryCommand(
        Long userId,
        int page,
        int size
) {
}