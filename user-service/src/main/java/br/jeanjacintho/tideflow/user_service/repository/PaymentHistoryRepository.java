package br.jeanjacintho.tideflow.user_service.repository;

import br.jeanjacintho.tideflow.user_service.model.PaymentHistory;
import br.jeanjacintho.tideflow.user_service.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, UUID> {
    
    Page<PaymentHistory> findByCompanyIdOrderByPaymentDateDesc(UUID companyId, Pageable pageable);
    
    List<PaymentHistory> findByCompanyIdOrderByPaymentDateDesc(UUID companyId);
    
    Optional<PaymentHistory> findByStripeInvoiceId(String stripeInvoiceId);
    
    List<PaymentHistory> findByCompanyIdAndStatusOrderByPaymentDateDesc(
        UUID companyId, 
        PaymentStatus status
    );
    
    @Query("SELECT SUM(p.amount) FROM PaymentHistory p WHERE p.company.id = :companyId AND p.status = :status")
    BigDecimal sumAmountByCompanyIdAndStatus(
        @Param("companyId") UUID companyId,
        @Param("status") PaymentStatus status
    );
    
    @Query("SELECT COUNT(p) FROM PaymentHistory p WHERE p.company.id = :companyId AND p.status = :status")
    Long countByCompanyIdAndStatus(
        @Param("companyId") UUID companyId,
        @Param("status") PaymentStatus status
    );
    
    @Query("SELECT p FROM PaymentHistory p WHERE p.company.id = :companyId " +
           "AND p.paymentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY p.paymentDate DESC")
    List<PaymentHistory> findByCompanyIdAndDateRange(
        @Param("companyId") UUID companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    boolean existsByStripeInvoiceId(String stripeInvoiceId);
}
