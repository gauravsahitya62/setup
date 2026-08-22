package com.flowset.marketplace.product.repository;

import com.flowset.marketplace.product.entity.ProductAttributeEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributeRepository extends JpaRepository<ProductAttributeEntity, UUID> {

    List<ProductAttributeEntity> findByProductIdOrderByDisplayOrderAsc(UUID productId);

    Optional<ProductAttributeEntity> findByIdAndProductId(UUID id, UUID productId);

    long countByProductId(UUID productId);
}
