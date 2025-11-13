package io.hhplus.ECommerce.ECommerce_project.product.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // ID로 조회 (삭제되지 않은 상품만)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Product> findByIdActive(@Param("id") Long id);

    // ID로 조회 + PESSIMISTIC LOCK (재고 차감용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    // 카테고리, 활성화 상품 개수
    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND (:categoryId IS NULL OR p.category.id = :categoryId) AND p.deletedAt IS NULL")
    long countActiveProducts(@Param("categoryId") Long categoryId);

    // 커스텀 필터, 정렬, 페이징 등은 Service에서 Pageable 사용
    List<Product> findByIsActiveTrueAndDeletedAtIsNull();

    // 상품 목록 조회 (카테고리 필터링, 정렬, 페이징)
    @Query("SELECT p FROM Product p WHERE (:categoryId IS NULL OR p.category.id = :categoryId) AND p.isActive = true AND p.deletedAt IS NULL")
    Page<Product> findProducts(@Param("categoryId") Long categoryId, Pageable pageable);
}
