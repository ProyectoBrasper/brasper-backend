package com.brasper.platform.treasury.domain.model.aggregates;

import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerContributionCommand;
import com.brasper.platform.treasury.domain.model.entities.PartnerContribution;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "partners", uniqueConstraints = @UniqueConstraint(
        name = Partner.FULL_NAME_UNIQUE_CONSTRAINT, columnNames = "full_name"))
public class Partner {

    public static final String FULL_NAME_UNIQUE_CONSTRAINT = "uk_partners_full_name";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(length = 100)
    private String role;

    // shortcut: eager contributions for small histories, use paged queries when histories grow.
    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PartnerContribution> contributions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Partner() {
    }

    public Partner(RegisterPartnerCommand command) {
        if (command.fullName() == null || command.fullName().isBlank() || command.fullName().strip().length() > 200) {
            throw new IllegalArgumentException("Full name is required and must not exceed 200 characters");
        }
        this.fullName = command.fullName().strip();
        this.role = command.role() == null || command.role().isBlank() ? null : command.role().strip();
        if (role != null && role.length() > 100) {
            throw new IllegalArgumentException("Role must not exceed 100 characters");
        }
    }

    public PartnerContribution registerContribution(RegisterPartnerContributionCommand command) {
        PartnerContribution contribution = new PartnerContribution(this, command);
        contributions.add(contribution);
        updatedAt = Instant.now();
        return contribution;
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

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public List<PartnerContribution> getContributions() {
        return List.copyOf(contributions);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
