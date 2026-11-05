package com.brasper.platform.projects.domain.model.aggregates;

import com.brasper.platform.projects.domain.model.commands.CreateClientCommand;
import com.brasper.platform.projects.domain.model.commands.UpdateClientCommand;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(nullable = false, unique = true, length = 11, updatable = false)
    private Ruc ruc;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Client() {
    }

    public Client(CreateClientCommand command) {
        this.ruc = Objects.requireNonNull(command.ruc(), "ruc");
        this.businessName = requireName(command.businessName());
        this.contactName = blankToNull(command.contactName());
        this.phone = blankToNull(command.phone());
        this.email = blankToNull(command.email());
    }

    public void update(UpdateClientCommand command) {
        this.businessName = requireName(command.businessName());
        this.contactName = blankToNull(command.contactName());
        this.phone = blankToNull(command.phone());
        this.email = blankToNull(command.email());
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

    public String getContactName() {
        return contactName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
