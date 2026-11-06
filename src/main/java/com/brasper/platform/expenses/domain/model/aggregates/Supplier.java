package com.brasper.platform.expenses.domain.model.aggregates;

import com.brasper.platform.expenses.domain.model.commands.CreateSupplierCommand;
import com.brasper.platform.expenses.domain.model.commands.UpdateSupplierCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.SupplierType;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
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
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(unique = true, length = 11, updatable = false)
    private Ruc ruc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private SupplierType type;

    @Column(length = 30)
    private String phone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Supplier() {
    }

    public Supplier(CreateSupplierCommand command) {
        this.type = Objects.requireNonNull(command.type(), "type");
        if (type == SupplierType.COMPANY && command.ruc() == null) {
            throw new IllegalArgumentException("A company supplier needs a RUC");
        }
        this.ruc = command.ruc();
        this.businessName = requireName(command.businessName());
        this.phone = blankToNull(command.phone());
    }

    public void update(UpdateSupplierCommand command) {
        this.businessName = requireName(command.businessName());
        this.phone = blankToNull(command.phone());
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Business name is required");
        }
        return name.strip();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
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

    public String getBusinessName() {
        return businessName;
    }

    public Ruc getRuc() {
        return ruc;
    }

    public SupplierType getType() {
        return type;
    }

    public String getPhone() {
        return phone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
