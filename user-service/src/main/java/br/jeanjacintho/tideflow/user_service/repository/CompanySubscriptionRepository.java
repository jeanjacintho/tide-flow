package br.jeanjacintho.tideflow.user_service.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.jeanjacintho.tideflow.user_service.model.CompanySubscription;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionStatus;

@Repository
public interface CompanySubscriptionRepository extends JpaRepository<CompanySubscription, UUID>, JpaSpecificationExecutor<CompanySubscription> {
    Optional<CompanySubscription> findByCompanyId(UUID companyId);
    
    Optional<CompanySubscription> findByStripeCustomerId(String stripeCustomerId);
    
    List<CompanySubscription> findByStatus(SubscriptionStatus status);
    
    @Query("SELECT s FROM CompanySubscription s WHERE s.nextBillingDate <= :date AND s.status = :status")
    List<CompanySubscription> findUpcomingBillings(@Param("date") LocalDate date, @Param("status") SubscriptionStatus status);
    
    @Query("SELECT s FROM CompanySubscription s WHERE s.status = :status AND s.nextBillingDate < :date")
    List<CompanySubscription> findExpiredSubscriptions(@Param("date") LocalDate date, @Param("status") SubscriptionStatus status);
}
