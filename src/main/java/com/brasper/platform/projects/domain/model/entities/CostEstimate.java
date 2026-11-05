package com.brasper.platform.projects.domain.model.entities;

import com.brasper.platform.projects.domain.model.aggregates.Project;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Estimated direct cost of one category. Its currency is the project's. Created only through
 * {@code Project.setCostEstimate}.
 */
@Entity
@Table(name = "project_cost_estimates")
public class CostEstimate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private CostCategory category;

    @Column(name = "estimated_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedAmount;

    protected CostEstimate() {
    }

    public CostEstimate(Project project, CostCategory category, BigDecimal estimatedAmount) {
        this.project = Objects.requireNonNull(project, "project");
        this.category = Objects.requireNonNull(category, "category");
        this.estimatedAmount = Objects.requireNonNull(estimatedAmount, "estimatedAmount");
    }

    public void changeAmount(BigDecimal estimatedAmount) {
        this.estimatedAmount = Objects.requireNonNull(estimatedAmount, "estimatedAmount");
    }

    public Long getId() {
        return id;
    }

    public CostCategory getCategory() {
        return category;
    }

    public BigDecimal getEstimatedAmount() {
        return estimatedAmount;
    }
}
