package com.flowset.marketplace.product.repository;

import com.flowset.marketplace.product.entity.ProductTagEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTagRepository extends JpaRepository<ProductTagEntity, UUID> {

    Optional<ProductTagEntity> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);
}
