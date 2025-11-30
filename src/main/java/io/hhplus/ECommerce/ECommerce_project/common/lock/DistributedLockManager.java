package io.hhplus.ECommerce.ECommerce_project.common.lock;

import io.hhplus.ECommerce.ECommerce_project.common.exception.BusinessException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.LockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 분산 락 매니저
 * Redisson의 Pub/Sub 방식을 사용하여 효율적인 락 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockManager {

    private final RedissonClient redissonClient;

    /**
     * 분산 락을 획득하고 비즈니스 로직 실행
     *
     * @param lockKey 락 키
     * @param waitTime 락 획득 대기 시간
     * @param leaseTime 락 자동 해제 시간(TTL)
     * @param timeUnit 시간 단위
     * @param supplier 실행할 비즈니스 로직(메서드)
     * @return 비스니스 로직 실행 결과
     */
    public <T> T executeWithLock(
            String lockKey,
            long waitTime,
            long leaseTime,
            TimeUnit timeUnit,
            Supplier<T> supplier
    ) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            // 락 획득 시도
            acquired = lock.tryLock(waitTime, leaseTime, timeUnit);

            if (!acquired) {
                log.warn("락 획득 실패: key={}, waitTime={}{}", lockKey, waitTime, timeUnit);
                throw new LockException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }

            log.info("락 획득 성공: key={}", lockKey);

            // 비즈니스 로직 실행
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("락 획득 중 인터럽트 발생: key={}", lockKey, e);
            throw new LockException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            // 락 해제 (현재 스레드가 소유한 경우에만)
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("락 해제 완료: key={}", lockKey);
            }
        }
    }

    /**
     * 기본 타임아웃 값으로 락 실행 (waitTime=2초, leaseTime=5초)
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, 2L, 5L, TimeUnit.SECONDS, supplier);
    }
}
