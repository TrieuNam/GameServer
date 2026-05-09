package com.SouthMillion.activity_service.entity;

import java.math.BigDecimal;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "box_fund")
public class BoxFund {

    @Id
    private Long id;

    private BigDecimal amount;

    // constructors
    public BoxFund() {}

    public BoxFund(Long id, BigDecimal amount) {
        this.id = id;
        this.amount = amount;
    }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}