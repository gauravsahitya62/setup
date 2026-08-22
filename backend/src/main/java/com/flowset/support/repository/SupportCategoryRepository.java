package com.flowset.support.repository;

import com.flowset.support.entity.SupportCategoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportCategoryRepository extends JpaRepository<SupportCategoryEntity, UUID> {

    List<SupportCategoryEntity> findAllByOrderByDisplayOrderAsc();
}
