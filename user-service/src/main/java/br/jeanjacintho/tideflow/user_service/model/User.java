package br.jeanjacintho.tideflow.user_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_company", columnList = "company_id"),
    @Index(name = "idx_user_department", columnList = "department_id"),
    @Index(name = "idx_user_anonymized", columnList = "anonymized_id"),
    @Index(name = "idx_user_active", columnList = "is_active")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "trusted_email", length = 255)
    private String trustedEmail;

    @Column(length = 100)
    private String city;

    @Column(length = 2)
    private String state;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false)
    private SystemRole systemRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "employee_id", length = 100)
    private String employeeId;

    @Column(name = "anonymized_id", unique = true, nullable = false)
    private UUID anonymizedId;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public User() {}

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public User(String name, String email, String password, String phone, String avatarUrl, String city, String state) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.city = city;
        this.state = state;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (mustChangePassword == null) {
            mustChangePassword = false;
        }
        if (anonymizedId == null) {
            anonymizedId = UUID.randomUUID();
        }
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (systemRole == null) {
            systemRole = SystemRole.NORMAL;
        }
        // Gera username único se não fornecido (para compatibilidade com registros antigos)
        if (username == null || username.isEmpty()) {
            // Gera username baseado em timestamp + random para garantir unicidade
            username = "user_" + System.currentTimeMillis() + "_" + 
                      String.valueOf(Math.abs(UUID.randomUUID().hashCode())).substring(0, 6);
        }
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getTrustedEmail() {
        return trustedEmail;
    }

    public void setTrustedEmail(String trustedEmail) {
        this.trustedEmail = trustedEmail;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public UUID getAnonymizedId() {
        return anonymizedId;
    }

    public void setAnonymizedId(UUID anonymizedId) {
        this.anonymizedId = anonymizedId;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public SystemRole getSystemRole() {
        return systemRole;
    }

    public void setSystemRole(SystemRole systemRole) {
        this.systemRole = systemRole;
    }
}
