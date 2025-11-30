package io.hhplus.ECommerce.ECommerce_project.common.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * 로컬 캐시 설정 (Caffeine)
 * - 애플리케이션 메모리에 캐시를 저장
 * - 빠른 응답 속도가 필요한 데이터에 적합
 * - 단일 인스턴스 환경에서 효과적
 */
@Configuration
@EnableCaching
public class LocalCacheConfig {

    /**
     * 로컬 캐시 매니저 (Caffeine)
     * - 동적으로 캐시 생성 가능 (@Cacheable의 value에 지정한 이름으로 자동 생성)
     * - 예: @Cacheable("categoryList"), @Cacheable("productList") 등
     */
    @Bean
    @Primary  // 기본 CacheManager로 설정
    public CacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)  // 5분 후 만료
                .maximumSize(100)  // 최대 100개 항목
                .recordStats());  // 캐시 통계 기록
        return cacheManager;
    }
}