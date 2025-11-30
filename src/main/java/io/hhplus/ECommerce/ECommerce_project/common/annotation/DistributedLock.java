package io.hhplus.ECommerce.ECommerce_project.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Redis 분산 락을 적용하기 위한 어노테이션
 * Redisson의 Pub/Sub 방식을 사용하여 효율적인 락 획득
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 락의 키 (SpEL 표현식 지원)
     * 예: "'coupon:issue:' + #command.couponId()"
     */
    String key();

    /**
     * 락 획득을 시도하는 최대 대기 시간 (기본값: 2초)
     * 이 시간 내에 락을 획득하지 못하면 예외 발생
     */
    long waitTime() default 2L;

    /**
     * 락을 획득한 후 자동으로 해제되는 시간 (기본값: 5초)
     * 데드락 방지를 위한 안전장치
     * 비즈니스 로직 실행 시간보다 충분히 길어야 함
     */
    long leaseTime() default 5L;

    /**
     * 시간 단위 (기본값: 초)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
