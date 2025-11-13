package io.hhplus.ECommerce.ECommerce_project.payment.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.payment.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 해당 주문의 결제 목록 조회
    List<Payment> findByOrder_Id(Long orderId);
}
