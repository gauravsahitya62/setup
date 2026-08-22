package com.flowset.orders.repository;

import com.flowset.orders.entity.OrderNumberCounterEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderNumberCounterRepository extends JpaRepository<OrderNumberCounterEntity, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OrderNumberCounterEntity c where c.year = :year")
    Optional<OrderNumberCounterEntity> lockByYear(@Param("year") int year);
}
