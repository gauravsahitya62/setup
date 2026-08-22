package com.flowset.marketplace.category.repository;

import com.flowset.marketplace.category.entity.CategoryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    @Query("select c from CategoryEntity c where lower(c.slug) = lower(:slug)")
    Optional<CategoryEntity> findBySlugIgnoreCase(@Param("slug") String slug);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);

    List<CategoryEntity> findByParentCategoryIsNullOrderByDisplayOrderAsc();

    List<CategoryEntity> findByParentCategoryIdOrderByDisplayOrderAsc(UUID parentCategoryId);

    List<CategoryEntity> findAllByOrderByDisplayOrderAsc();

    long countByParentCategoryId(UUID parentCategoryId);
}
