package io.hhplus.ECommerce.ECommerce_project.cart.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // 해당 유저의 전체 장바구니 목록 가져오기
    List<Cart> findAllByUser_Id(Long userId);

    // 해당 유저의 상품 장바구니 단건 조회
    Optional<Cart> findByUser_IdAndProduct_Id(Long userId, Long productId);

    // 장바구니 목록을 상품과 함께 가져옴 -> N+1 문제 해결
    @Query("SELECT c FROM Cart c JOIN FETCH c.product WHERE c.user.id = :userId")
    List<Cart> findAllByUserIdWithProduct(@Param("userId") Long userId);
}
