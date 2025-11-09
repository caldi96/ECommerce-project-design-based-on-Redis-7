package io.hhplus.ECommerce.ECommerce_project.point.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class PointUsageEntityTest {

    @Test
    void createPointUsageHistory_Success() {
        PointUsageHistory history = PointUsageHistory.create(1L, 10L, BigDecimal.valueOf(500));

        assertNotNull(history);
        assertEquals(1L, history.getPointId());
        assertEquals(10L, history.getOrderId());
        assertEquals(BigDecimal.valueOf(500), history.getUsedAmount());
        assertNotNull(history.getCreatedAt());
        assertNull(history.getCanceledAt());
        assertTrue(history.isValid());
        assertFalse(history.isCanceled());
    }

    @Test
    void createPointUsageHistory_NullPointId_ThrowsException() {
        PointException exception = assertThrows(PointException.class,
                () -> PointUsageHistory.create(null, 10L, BigDecimal.valueOf(500)));
        assertTrue(exception.getMessage().contains("포인트 ID는 필수입니다."));
    }

    @Test
    void cancelUsage_Success() {
        PointUsageHistory history = PointUsageHistory.create(1L, 10L, BigDecimal.valueOf(500));
        history.cancel();

        assertNotNull(history.getCanceledAt());
        assertTrue(history.isCanceled());
        assertFalse(history.isValid());
    }

    @Test
    void cancelUsage_AlreadyCanceled_ThrowsException() {
        PointUsageHistory history = PointUsageHistory.create(1L, 10L, BigDecimal.valueOf(500));
        history.cancel();

        PointException exception = assertThrows(PointException.class, history::cancel);
        assertTrue(exception.getMessage().contains("이미 취소된 포인트 사용 이력입니다."));
    }

    @Test
    void setId_Success() {
        PointUsageHistory history = PointUsageHistory.create(1L, 10L, BigDecimal.valueOf(500));
        history.setId(100L);

        assertEquals(100L, history.getId());
    }
}
