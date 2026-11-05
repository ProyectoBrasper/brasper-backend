package com.brasper.platform.projects.domain.model.entities;

import com.brasper.platform.projects.domain.model.aggregates.Project;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

/** Progress earned in one month. Created only through {@code Project.registerProgress}. */
@Entity
@Table(name = "project_progress")
public class ProgressRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Column(name = "period_year", nullable = false, updatable = false)
    private int year;

    @Column(name = "period_month", nullable = false, updatable = false)
    private int month;

    @Column(nullable = false, updatable = false, precision = 5, scale = 4)
    private Percentage percentage;

    protected ProgressRecord() {
    }

    public ProgressRecord(Project project, Period period, Percentage percentage) {
        this.project = Objects.requireNonNull(project, "project");
        this.year = period.year();
        this.month = period.month();
        this.percentage = Objects.requireNonNull(percentage, "percentage");
    }

    public Long getId() {
        return id;
    }

    public Period getPeriod() {
        return new Period(year, month);
    }

    public Percentage getPercentage() {
        return percentage;
    }
}
