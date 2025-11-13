package io.hhplus.ECommerce.ECommerce_project.category.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // ID로 조회 (삭제되지 않은 카테고리만)
    Optional<Category> findByIdAndDeletedAtIsNull(Long id);

    // 삭제되지 않은 전체 카테고리 조회
    List<Category> findAllByDeletedAtIsNull();

    // 카테고리명 중복 체크(삭제되지 않은 것만)
    boolean existsByCategoryNameAndDeletedAtIsNull(String name);

    // 표시 순서 중복 체크(삭제되지 않은 것만)
    boolean existsByDisplayOrderAndDeletedAtIsNull(int displayOrder);

    // 서로 다른 id를 가진 카테고리중 같은 이름이 있는지 확인(삭제되지 않은 것만)
    boolean existsByCategoryNameAndIdNotAndDeletedAtIsNull(String categoryName, Long excludedId);

    // 서로 다른 id를 가진 카테고리중 같은 표시 순서가 있는지 확인(삭제되지 않은 것만)
    boolean existsByDisplayOrderAndIdNotAndDeletedAtIsNull(int displayOrder, Long excludedId);


}
