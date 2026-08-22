package com.flowset.marketplace.collection.repository;

import com.flowset.marketplace.collection.entity.ProductCollectionItemEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCollectionItemRepository extends JpaRepository<ProductCollectionItemEntity, UUID> {

    List<ProductCollectionItemEntity> findByCollectionIdOrderByDisplayOrderAsc(UUID collectionId);

    Optional<ProductCollectionItemEntity> findByCollectionIdAndProductId(UUID collectionId, UUID productId);

    boolean existsByCollectionIdAndProductId(UUID collectionId, UUID productId);

    long countByCollectionId(UUID collectionId);
}
