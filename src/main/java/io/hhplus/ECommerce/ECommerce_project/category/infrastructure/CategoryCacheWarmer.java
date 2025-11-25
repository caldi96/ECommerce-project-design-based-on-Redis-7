package io.hhplus.ECommerce.ECommerce_project.category.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.category.application.GetCategoryListUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryCacheWarmer {

    private final GetCategoryListUseCase getCategoryListUseCase;

    /**
     * 애플리케이션 시작 시 카테고리 목록을 캐시에 미리 로드
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        log.info("Starting category cache warm-up...");
        try {
            getCategoryListUseCase.execute();
            log.info("Category cache warmed up successfully");
        } catch (Exception e) {
            log.error("Failed to warm up category cache", e);
        }
    }
}