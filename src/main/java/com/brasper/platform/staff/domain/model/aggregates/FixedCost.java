package com.brasper.platform.staff.domain.model.aggregates;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.staff.domain.model.commands.*;
import com.brasper.platform.staff.domain.model.entities.FixedCostPayment;
import com.brasper.platform.staff.domain.model.valueobjects.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "fixed_costs", schema = "brasper",
        uniqueConstraints = @UniqueConstraint(name = FixedCost.CODE_UNIQUE_CONSTRAINT, columnNames = "code"))
public class FixedCost {
    public static final String CODE_UNIQUE_CONSTRAINT = "uk_fixed_costs_code";
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 30, updatable = false)
    private CostCode code;
    @Column(nullable = false, length = 500)
    private String concept;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30, updatable = false)
    private FixedCostType type;
    @Column(name = "monthly_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 3, updatable = false)
    private Currency currency;
    @Column(name = "employee_id", updatable = false)
    private Long employeeId;
    @ElementCollection
    @CollectionTable(name = "fixed_cost_expected_weeks", schema = "brasper", joinColumns = @JoinColumn(name = "fixed_cost_id"))
    @Column(name = "expected_week", nullable = false)
    @OrderColumn(name = "week_order")
    private List<Integer> expectedWeeks = new ArrayList<>();
    @Column(nullable = false)
    private boolean active = true;
    @OneToMany(mappedBy = "fixedCost", cascade = CascadeType.ALL)
    private List<FixedCostPayment> payments = new ArrayList<>();
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FixedCost() {}
    public FixedCost(CreateFixedCostCommand command) {
        code = Objects.requireNonNull(command.code(), "code");
        type = Objects.requireNonNull(command.type(), "type");
        employeeId = command.employeeId();
        if (type == FixedCostType.SALARY && employeeId == null) throw new IllegalArgumentException("Salary requires an employee");
        currency = Objects.requireNonNull(command.monthlyAmount(), "monthlyAmount").currency();
        update(new UpdateFixedCostCommand(null, command.concept(), command.monthlyAmount(), command.schedule()));
    }
    public void update(UpdateFixedCostCommand command) {
        if (command.concept() == null || command.concept().isBlank() || command.concept().strip().length() > 500) {
            throw new IllegalArgumentException("Concept is required and must not exceed 500 characters");
        }
        Money amount = Objects.requireNonNull(command.monthlyAmount(), "monthlyAmount");
        if (!amount.isPositive() || amount.amount().precision() > 14 || amount.currency() != currency) {
            throw new IllegalArgumentException("Monthly amount must be positive, fit numeric(14,2) and retain currency");
        }
        PaymentSchedule schedule = Objects.requireNonNull(command.schedule(), "schedule");
        concept = command.concept().strip(); monthlyAmount = amount.amount();
        expectedWeeks.clear(); expectedWeeks.addAll(schedule.expectedWeeks());
        updatedAt = Instant.now();
    }
    public void deactivate() { active = false; updatedAt = Instant.now(); }
    public FixedCostPayment registerPayment(RegisterFixedCostPaymentCommand command) {
        if (!active) throw new IllegalStateException("Fixed cost is inactive");
        FixedCostPayment payment = new FixedCostPayment(this, command);
        payments.add(payment); updatedAt = Instant.now();
        return payment;
    }
    public Money paidIn(Period period) {
        Objects.requireNonNull(period, "period");
        return payments.stream().filter(p -> p.getPeriod().equals(period))
                .map(FixedCostPayment::getAmount).reduce(Money.zero(currency), Money::add);
    }
    public Money pendingIn(Period period) {
        Money pending = getMonthlyAmount().subtract(paidIn(period));
        return pending.isPositive() ? pending : Money.zero(currency);
    }
    public Money getMonthlyAmount() { return new Money(monthlyAmount, currency); }
    public PaymentSchedule getSchedule() { return new PaymentSchedule(expectedWeeks); }
    public List<Integer> getExpectedWeeks() { return List.copyOf(expectedWeeks); }
    public List<FixedCostPayment> getPayments() { return List.copyOf(payments); }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public CostCode getCode() { return code; }
    public String getConcept() { return concept; }
    public FixedCostType getType() { return type; }
    public Currency getCurrency() { return currency; }
    public Long getEmployeeId() { return employeeId; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
