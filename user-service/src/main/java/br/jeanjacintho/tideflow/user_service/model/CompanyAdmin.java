package br.jeanjacintho.tideflow.user_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_admins", indexes = {
    @Index(name = "idx_admin_user", columnList = "user_id"),
    @Index(name = "idx_admin_company", columnList = "company_id"),
    @Index(name = "idx_admin_user_company", columnList = "user_id, company_id"),
    @Index(name = "idx_admin_role", columnList = "role")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_admin_user_company", columnNames = {"user_id", "company_id"})
})
public class CompanyAdmin {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyAdminRole role;

    @Column(columnDefinition = "TEXT")
    private String permissions;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CompanyAdmin() {}

    public CompanyAdmin(User user, Company company, CompanyAdminRole role) {
        this.user = user;
        this.company = company;
        this.role = role;
    }

    public CompanyAdmin(User user, Company company, CompanyAdminRole role, String permissions) {
        this.user = user;
        this.company = company;
        this.role = role;
        this.permissions = permissions;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public CompanyAdminRole getRole() {
        return role;
    }

    public void setRole(CompanyAdminRole role) {
        this.role = role;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
