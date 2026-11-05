package com.brasper.platform.projects.domain.model.aggregates;

import com.brasper.platform.projects.domain.model.commands.CreateProjectCommand;
import com.brasper.platform.projects.domain.model.commands.UpdateProjectCommand;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectCode;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectStatus;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/** Progress records and cost estimates are added to this aggregate in later steps. */
@Entity
@Table(name = "projects")
public class Project {

    private static final Percentage ONE = new Percentage(BigDecimal.ONE);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 14, updatable = false)
    private ProjectCode code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Other aggregates are referenced by id only. */
    @Column(name = "client_id", nullable = false, updatable = false)
    private Long clientId;

    /** Taxable base, without IGV. Kept as amount + currency so {@code Money} stays free of JPA. */
    @Column(name = "budget_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal budgetAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3, updatable = false)
    private Currency currency;

    @Column(name = "quoted_on", nullable = false)
    private LocalDate quotedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.QUOTED;

    /** 1.0 own project, 0.5 shared (Linings). */
    @Column(nullable = false, precision = 5, scale = 4)
    private Percentage participation;

    @Column(name = "sales_commission_rate", nullable = false, precision = 5, scale = 4)
    private Percentage salesCommissionRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Project() {
    }

    public Project(CreateProjectCommand command) {
        this.code = Objects.requireNonNull(command.code(), "code");
        this.clientId = Objects.requireNonNull(command.clientId(), "clientId");
        this.quotedOn = Objects.requireNonNull(command.quotedOn(), "quotedOn");
        this.currency = Objects.requireNonNull(command.budget(), "budget").currency();
        apply(command.name(), command.description(), command.budget().amount(), command.participation(),
                command.salesCommissionRate());
    }

    public void update(UpdateProjectCommand command) {
        requireOpen("edited");
        apply(command.name(), command.description(), command.budget(), command.participation(),
                command.salesCommissionRate());
    }

    /** Quoted to in progress. */
    public void start() {
        if (status != ProjectStatus.QUOTED) {
            throw new IllegalStateException("Only a quoted project can be started, but it is " + status);
        }
        status = ProjectStatus.IN_PROGRESS;
    }

    public void cancel() {
        requireOpen("cancelled");
        status = ProjectStatus.CANCELLED;
    }

    private void requireOpen(String action) {
        if (!status.isOpen()) {
            throw new IllegalStateException("A %s project cannot be %s".formatted(status, action));
        }
    }

    private void apply(String name, String description, BigDecimal budget, Percentage participation,
            Percentage salesCommissionRate) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name is required");
        }
        if (Objects.requireNonNull(budget, "budget").signum() <= 0) {
            throw new IllegalArgumentException("Budget must be greater than zero");
        }
        if (Objects.requireNonNull(participation, "participation").value().signum() == 0
                || participation.value().compareTo(ONE.value()) > 0) {
            throw new IllegalArgumentException("Participation must be above 0 and at most 1");
        }
        if (Objects.requireNonNull(salesCommissionRate, "salesCommissionRate").value()
                .compareTo(ONE.value()) > 0) {
            throw new IllegalArgumentException("Sales commission rate cannot exceed 1");
        }
        this.name = name.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.budgetAmount = budget;
        this.participation = participation;
        this.salesCommissionRate = salesCommissionRate;
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ProjectCode getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getClientId() {
        return clientId;
    }

    public Money getBudget() {
        return new Money(budgetAmount, currency);
    }

    public LocalDate getQuotedOn() {
        return quotedOn;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public Percentage getParticipation() {
        return participation;
    }

    public Percentage getSalesCommissionRate() {
        return salesCommissionRate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
