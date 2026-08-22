package com.flowset.marketplace.product.repository;

import com.flowset.marketplace.product.entity.ProductImageEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, UUID> {

    List<ProductImageEntity> findByProductIdOrderByDisplayOrderAsc(UUID productId);

    Optional<ProductImageEntity> findByIdAndProductId(UUID id, UUID productId);

    long countByProductId(UUID productId);
}
