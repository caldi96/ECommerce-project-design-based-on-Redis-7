package io.hhplus.ECommerce.ECommerce_project.common.aop;

import io.hhplus.ECommerce.ECommerce_project.common.annotation.DistributedLock;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.LockException;
import io.hhplus.ECommerce.ECommerce_project.common.transaction.RequireNewTransactionAspect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final RequireNewTransactionAspect requireNewTransactionAspect;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer defaultParameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private static final String REDISSON_LOCK_PREFIX = "LOCK:";

    /**
     * `@DistributedLock` 어노테이션이 선언된 메소드를 포인트컷으로 설정
     *
     * @param distributedLock 분산락 처리를 위한 어노테이션
     */
    @Pointcut("@annotation(distributedLock)")
    public void pointCut(DistributedLock distributedLock) {}

    /**
     * 분산 락을 사용하여 메소드를 감싸는 Around 어드바이스
     *
     * @param pjp ProceedingJoinPoint, 원래의 메소드를 나타냄
     * @param distributedLock 분산락 어노테이션
     * @return 메소드 실행 결과
     * @throws Throwable 예외 처리
     */
    @Around(value = "pointCut(distributedLock)", argNames = "pjp,distributedLock")
    public Object around(ProceedingJoinPoint pjp, DistributedLock distributedLock) throws Throwable {

        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String lockKey = generateLockKey(distributedLock.key(), method, pjp.getArgs());

        RLock lock = redissonClient.getLock(REDISSON_LOCK_PREFIX + lockKey);

        boolean acquired = false;
        try {
            acquired = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (!acquired) {
                log.warn("락 획득 실패: key={}, waitTime={}{}", lockKey, distributedLock.waitTime(), distributedLock.timeUnit());
                throw new LockException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }

            log.info("락 획득 성공: key={}", lockKey);

            // 새로운 트랜잭션으로 비즈니스 실행
            return requireNewTransactionAspect.execute(pjp);
        } catch (InterruptedException e) {
            // 락 획득 중 인터럽트 발생 시 처리
            log.error("락 획득 중 인터럽트 발생: key={}", lockKey, e);
            Thread.currentThread().interrupt(); // 현재 스레드의 인터럽트 상태 복구
            throw new LockException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            // 분산락 해제
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Redis 락 해제 완료: key={}", lockKey);
            }
        }
    }

    private String generateLockKey(String keyExpression, Method method, Object[] args) {
        EvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = defaultParameterNameDiscoverer.getParameterNames(method);

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        return expressionParser.parseExpression(keyExpression).getValue(context, String.class);
    }
}
