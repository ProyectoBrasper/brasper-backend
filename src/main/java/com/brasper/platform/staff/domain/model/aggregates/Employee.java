package com.brasper.platform.staff.domain.model.aggregates;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.staff.domain.model.commands.HireEmployeeCommand;
import com.brasper.platform.staff.domain.model.commands.UpdateEmployeeCommand;
import com.brasper.platform.staff.domain.model.valueobjects.Dni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "employees", schema = "brasper", uniqueConstraints = @UniqueConstraint(
        name = Employee.DNI_UNIQUE_CONSTRAINT, columnNames = "dni"))
public class Employee {
    public static final String DNI_UNIQUE_CONSTRAINT = "uk_employees_dni";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "full_name", nullable = false, length = 200, updatable = false) private String fullName;
    @Column(nullable = false, length = 8, updatable = false) private Dni dni;
    @Column(length = 100) private String position;
    @Column(name = "monthly_salary", nullable = false, precision = 14, scale = 2) private BigDecimal monthlySalary;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 3) private Currency currency;
    @Column(name = "hired_on", nullable = false, updatable = false) private LocalDate hiredOn;
    @Column(name = "terminated_on") private LocalDate terminatedOn;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Employee() { }
    public Employee(HireEmployeeCommand command) {
        if (command.fullName() == null || command.fullName().isBlank() || command.fullName().strip().length() > 200) {
            throw new IllegalArgumentException("Full name is required and must not exceed 200 characters");
        }
        fullName = command.fullName().strip();
        dni = Objects.requireNonNull(command.dni(), "dni");
        hiredOn = Objects.requireNonNull(command.hiredOn(), "hiredOn");
        update(new UpdateEmployeeCommand(null, command.position(), command.monthlySalary()));
        active = true;
    }
    public void update(UpdateEmployeeCommand command) {
        if (terminatedOn != null) throw new IllegalStateException("Employee is terminated");
        Money salary = Objects.requireNonNull(command.monthlySalary(), "monthlySalary");
        if (!salary.isPositive() || salary.amount().precision() > 14) {
            throw new IllegalArgumentException("Monthly salary must be positive and fit numeric(14,2)");
        }
        String newPosition = command.position() == null || command.position().isBlank() ? null : command.position().strip();
        if (newPosition != null && newPosition.length() > 100) {
            throw new IllegalArgumentException("Position must not exceed 100 characters");
        }
        position = newPosition;
        monthlySalary = salary.amount();
        currency = salary.currency();
    }
    public void terminate(LocalDate date) {
        if (!active) throw new IllegalStateException("Employee is already terminated");
        Objects.requireNonNull(date, "terminatedOn");
        if (date.isBefore(hiredOn)) throw new IllegalArgumentException("Termination cannot precede hiring");
        terminatedOn = date;
        active = false;
    }
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public Dni getDni() { return dni; }
    public String getPosition() { return position; }
    public LocalDate getHiredOn() { return hiredOn; }
    public LocalDate getTerminatedOn() { return terminatedOn; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Money getMonthlySalary() { return new Money(monthlySalary, currency); }
    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
