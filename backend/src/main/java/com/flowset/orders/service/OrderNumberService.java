package com.flowset.orders.service;

import com.flowset.orders.entity.OrderNumberCounterEntity;
import com.flowset.orders.repository.OrderNumberCounterRepository;
import java.time.Year;
import java.time.ZoneOffset;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderNumberService {

    private final OrderNumberCounterRepository counterRepository;

    public OrderNumberService(OrderNumberCounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    @Transactional
    public String nextNumber() {
        int year = Year.now(ZoneOffset.UTC).getValue();
        OrderNumberCounterEntity counter = lockOrCreate(year);
        int next = counter.getLastValue() + 1;
        counter.setLastValue(next);
        counterRepository.save(counter);
        return "FS-%d-%06d".formatted(year, next);
    }

    private OrderNumberCounterEntity lockOrCreate(int year) {
        return counterRepository.lockByYear(year).orElseGet(() -> {
            try {
                OrderNumberCounterEntity created = new OrderNumberCounterEntity();
                created.setYear(year);
                created.setLastValue(0);
                counterRepository.saveAndFlush(created);
            } catch (DataIntegrityViolationException ignored) {
                // another transaction inserted the year row
            }
            return counterRepository
                    .lockByYear(year)
                    .orElseThrow(() -> new IllegalStateException("Unable to allocate order number"));
        });
    }
}
