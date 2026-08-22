package com.flowset.orders.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_number_counters")
public class OrderNumberCounterEntity {

    @Id
    @Column(name = "counter_year")
    private int year;

    @Column(name = "last_value", nullable = false)
    private int lastValue;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getLastValue() {
        return lastValue;
    }

    public void setLastValue(int lastValue) {
        this.lastValue = lastValue;
    }
}
