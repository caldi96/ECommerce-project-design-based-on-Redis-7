package io.hhplus.ECommerce.ECommerce_project.point.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import io.hhplus.ECommerce.ECommerce_project.point.domain.enums.PointType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class PointEntityTest {

    @Test
    void chargePoint_Success() {
        Point point = Point.charge(1L, BigDecimal.valueOf(1000), "충전");

        assertNotNull(point);
        assertEquals(1L, point.getUserId());
        assertEquals(BigDecimal.valueOf(1000), point.getAmount());
        assertEquals(PointType.CHARGE, point.getPointType());
        assertFalse(point.isUsed());
        assertFalse(point.isExpired());
        assertTrue(point.getRemainingAmount().compareTo(BigDecimal.valueOf(1000)) == 0);
    }

    @Test
    void usePoint_Success() {
        Point point = Point.use(1L, BigDecimal.valueOf(500), "사용");

        assertNotNull(point);
        assertEquals(PointType.USE, point.getPointType());
        assertEquals(BigDecimal.valueOf(500), point.getAmount());
        assertTrue(point.isUsed());
        assertEquals(BigDecimal.valueOf(0), point.getRemainingAmount());
    }

    @Test
    void refundPoint_Success() {
        Point point = Point.refund(1L, BigDecimal.valueOf(200), "환불");

        assertNotNull(point);
        assertEquals(PointType.REFUND, point.getPointType());
        assertEquals(BigDecimal.valueOf(200), point.getAmount());
        assertFalse(point.isUsed());
        assertFalse(point.isExpired());
    }

    @Test
    void expirePoint_Success() {
        Point point = Point.charge(1L, BigDecimal.valueOf(1000), "충전");
        point.expire();

        assertTrue(point.isExpired());
    }

    @Test
    void usePartially_Success() {
        Point point = Point.charge(1L, BigDecimal.valueOf(1000), "충전");
        point.usePartially(BigDecimal.valueOf(400));

        assertEquals(BigDecimal.valueOf(400), point.getUsedAmount());
        assertFalse(point.isUsed());
        assertEquals(BigDecimal.valueOf(600), point.getRemainingAmount());
    }

    @Test
    void usePartially_FullUse_SetsUsedFlag() {
        Point point = Point.charge(1L, BigDecimal.valueOf(500), "충전");
        point.usePartially(BigDecimal.valueOf(500));

        assertTrue(point.isUsed());
        assertEquals(BigDecimal.valueOf(0), point.getRemainingAmount());
    }

    @Test
    void markAsUsed_SetsUsedAmount() {
        Point point = Point.charge(1L, BigDecimal.valueOf(300), "충전");
        point.markAsUsed();

        assertTrue(point.isUsed());
        assertEquals(BigDecimal.valueOf(300), point.getUsedAmount());
        assertEquals(BigDecimal.valueOf(0), point.getRemainingAmount());
    }

    @Test
    void restoreUsedAmount_Success() {
        Point point = Point.charge(1L, BigDecimal.valueOf(500), "충전");
        point.usePartially(BigDecimal.valueOf(300));
        point.restoreUsedAmount(BigDecimal.valueOf(200));

        assertEquals(BigDecimal.valueOf(100), point.getUsedAmount());
        assertFalse(point.isUsed());
        assertEquals(BigDecimal.valueOf(400), point.getRemainingAmount());
    }

    @Test
    void isAvailable_ChecksCorrectly() {
        Point point = Point.charge(1L, BigDecimal.valueOf(1000), "충전");
        assertTrue(point.isAvailable());

        point.markAsUsed();
        assertFalse(point.isAvailable());

        Point expiredPoint = Point.charge(1L, BigDecimal.valueOf(100), "충전");
        expiredPoint.expire();
        assertFalse(expiredPoint.isAvailable());
    }
}
