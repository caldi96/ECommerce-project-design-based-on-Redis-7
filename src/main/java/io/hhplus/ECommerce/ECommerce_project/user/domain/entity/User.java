package io.hhplus.ECommerce.ECommerce_project.user.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.entity.BaseEntity;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String username;            // 로그인 id

    @Column(nullable = false)
    private String password;            // 로그인 password

    @Column(name = "point_balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal pointBalance = BigDecimal.ZERO;    // 포인트 잔액

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;    // 생성일

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;    // 수정일

    @Version
    private Long version;

    /**
     * 포인트 잔액 비교
     */
    public boolean hasEnoughPoint(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        // compareTo: 0 = 같음, 1 = this > amount, -1 = this < amount
        return this.pointBalance.compareTo(amount) >= 0;
    }

    /**
     * 포인트 사용
     */
    public void usePoint(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PointException(ErrorCode.POINT_AMOUNT_INVALID);
        }

        if (!hasEnoughPoint(amount)) {
            throw new PointException(ErrorCode.POINT_INSUFFICIENT_POINT,
                "포인트가 부족합니다. 현재 잔액: " + this.pointBalance + ", 사용 요청: " + amount);
        }

        this.pointBalance = this.pointBalance.subtract(amount);
    }

    /**
     * 포인트 충전
     */
    public void chargePoint(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PointException(ErrorCode.POINT_AMOUNT_INVALID);
        }

        this.pointBalance = this.pointBalance.add(amount);
    }

    /**
     * 포인트 환불
     */
    public void refundPoint(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PointException(ErrorCode.POINT_AMOUNT_INVALID);
        }

        this.pointBalance = this.pointBalance.add(amount);
    }

    /**
     * 포인트 만료
     */
    public void expirePoint(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PointException(ErrorCode.POINT_AMOUNT_INVALID);
        }

        // 만료 금액이 잔액보다 큰 경우
        if (this.pointBalance.compareTo(amount) < 0) {
            // 잔액을 0으로 설정 (음수 방지)
            this.pointBalance = BigDecimal.ZERO;
        } else {
            this.pointBalance = this.pointBalance.subtract(amount);
        }
    }
}
