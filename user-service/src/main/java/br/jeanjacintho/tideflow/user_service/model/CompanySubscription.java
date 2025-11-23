package br.jeanjacintho.tideflow.user_service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_subscriptions", indexes = {
    @Index(name = "idx_subscription_company", columnList = "company_id"),
    @Index(name = "idx_subscription_status", columnList = "status"),
    @Index(name = "idx_subscription_next_billing", columnList = "next_billing_date")
})
public class CompanySubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private SubscriptionPlan planType;

    @Column(name = "price_per_user", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerUser;

    @Column(name = "total_users", nullable = false)
    private Integer totalUsers;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    @Column(name = "next_billing_date", nullable = false)
    private LocalDate nextBillingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CompanySubscription() {}

    public CompanySubscription(Company company, SubscriptionPlan planType, BigDecimal pricePerUser, 
                              Integer totalUsers, BillingCycle billingCycle, LocalDate nextBillingDate) {
        this.company = company;
        this.planType = planType;
        this.pricePerUser = pricePerUser;
        this.totalUsers = totalUsers;
        this.billingCycle = billingCycle;
        this.nextBillingDate = nextBillingDate;
        this.status = SubscriptionStatus.TRIAL;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = SubscriptionStatus.TRIAL;
        }
        if (totalUsers == null) {
            totalUsers = 0;
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

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public SubscriptionPlan getPlanType() {
        return planType;
    }

    public void setPlanType(SubscriptionPlan planType) {
        this.planType = planType;
    }

    public BigDecimal getPricePerUser() {
        return pricePerUser;
    }

    public void setPricePerUser(BigDecimal pricePerUser) {
        this.pricePerUser = pricePerUser;
    }

    public Integer getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Integer totalUsers) {
        this.totalUsers = totalUsers;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    public LocalDate getNextBillingDate() {
        return nextBillingDate;
    }

    public void setNextBillingDate(LocalDate nextBillingDate) {
        this.nextBillingDate = nextBillingDate;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
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
