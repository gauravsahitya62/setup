package com.flowset.users.repository;

import com.flowset.common.RoleName;
import com.flowset.users.entity.UserEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {

    @Query("select u from UserEntity u where lower(u.email) = lower(:email) and u.deletedAt is null")
    Optional<UserEntity> findActiveByEmail(@Param("email") String email);

    @Query("select case when count(u) > 0 then true else false end from UserEntity u "
            + "where lower(u.email) = lower(:email) and u.deletedAt is null")
    boolean existsActiveByEmail(@Param("email") String email);

    @Query("select u from UserEntity u where u.id = :id and u.deletedAt is null")
    Optional<UserEntity> findActiveById(@Param("id") UUID id);

    @Query("""
            select u from UserEntity u
            where u.role = :role
              and u.deletedAt is null
              and u.status = com.flowset.common.UserStatus.ACTIVE
            """)
    List<UserEntity> findActiveByRole(@Param("role") RoleName role);

    long countByRoleAndDeletedAtIsNull(RoleName role);

    long countByRoleAndCreatedAtGreaterThanEqualAndDeletedAtIsNull(RoleName role, Instant from);

    long countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(Instant from);

    List<UserEntity> findByCreatedAtGreaterThanEqualAndDeletedAtIsNullOrderByCreatedAtAsc(Instant from);
}
