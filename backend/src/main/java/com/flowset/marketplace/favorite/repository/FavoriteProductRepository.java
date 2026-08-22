package com.flowset.marketplace.favorite.repository;

import com.flowset.marketplace.favorite.entity.FavoriteProductEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteProductRepository extends JpaRepository<FavoriteProductEntity, UUID> {

    boolean existsByPlannerUserIdAndProductId(UUID plannerUserId, UUID productId);

    Optional<FavoriteProductEntity> findByPlannerUserIdAndProductId(UUID plannerUserId, UUID productId);

    Page<FavoriteProductEntity> findByPlannerUserId(UUID plannerUserId, Pageable pageable);

    void deleteByPlannerUserIdAndProductId(UUID plannerUserId, UUID productId);

    long countByProductId(UUID productId);
}
