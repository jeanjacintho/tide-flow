package br.jeanjacintho.tideflow.user_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.jeanjacintho.tideflow.user_service.exception.AccessDeniedException;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.BillingCycle;
import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.CompanyStatus;
import br.jeanjacintho.tideflow.user_service.model.CompanySubscription;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionPlan;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionStatus;
import br.jeanjacintho.tideflow.user_service.repository.CompanyRepository;
import br.jeanjacintho.tideflow.user_service.repository.CompanySubscriptionRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingService Tests")
@SuppressWarnings("null")
class BillingServiceTest {

    @Mock
    private CompanySubscriptionRepository subscriptionRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyAuthorizationService authorizationService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private UsageTrackingService usageTrackingService;

    @InjectMocks
    private BillingService billingService;

    private Company testCompany;
    private CompanySubscription testSubscription;
    private UUID testCompanyId;

    @BeforeEach
    void setUp() {
        testCompanyId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        testCompany = new Company();
        testCompany.setId(testCompanyId);
        testCompany.setName("Test Company");
        testCompany.setDomain("test.com");
        testCompany.setSubscriptionPlan(SubscriptionPlan.ENTERPRISE);
        testCompany.setMaxEmployees(100);
        testCompany.setStatus(CompanyStatus.ACTIVE);
        testCompany.setBillingEmail("billing@test.com");

        testSubscription = new CompanySubscription();
        testSubscription.setId(UUID.randomUUID());
        testSubscription.setCompany(testCompany);
        testSubscription.setPlanType(SubscriptionPlan.ENTERPRISE);
        testSubscription.setPricePerUser(new BigDecimal("199.90"));
        testSubscription.setTotalUsers(10);
        testSubscription.setBillingCycle(BillingCycle.MONTHLY);
        testSubscription.setStatus(SubscriptionStatus.ACTIVE);
        testSubscription.setNextBillingDate(now.plusMonths(1));
    }

    @Test
    @DisplayName("generateInvoice - Deve gerar fatura com sucesso")
    void testGenerateInvoiceSuccess() {
        String period = "2024-01";
        int activeUsers = 10;
        BigDecimal totalAmount = new BigDecimal("1999.00");

        when(companyRepository.findById(testCompanyId)).thenReturn(Optional.of(testCompany));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(true);
        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(usageTrackingService.getActiveUserCount(testCompanyId)).thenReturn(activeUsers);
        when(subscriptionService.calculateMonthlyBill(testCompanyId)).thenReturn(totalAmount);

        BillingService.InvoiceDTO result = billingService.generateInvoice(testCompanyId, period);

        assertNotNull(result);
        assertEquals(testCompanyId, result.getCompanyId());
        assertEquals("Test Company", result.getCompanyName());
        assertEquals(period, result.getPeriod());
        assertEquals(LocalDate.of(2024, 1, 1), result.getStartDate());
        assertEquals(LocalDate.of(2024, 1, 31), result.getEndDate());
        assertEquals(SubscriptionPlan.ENTERPRISE.name(), result.getPlanType());
        assertEquals(activeUsers, result.getActiveUsers());
        assertEquals(new BigDecimal("199.90"), result.getPricePerUser());
        assertEquals(totalAmount, result.getTotalAmount());
        assertEquals(BillingCycle.MONTHLY.name(), result.getBillingCycle());
        assertEquals(SubscriptionStatus.ACTIVE.name(), result.getStatus());

        verify(companyRepository).findById(testCompanyId);
        verify(authorizationService).canAccessCompany(testCompanyId);
        verify(subscriptionRepository).findByCompanyId(testCompanyId);
        verify(usageTrackingService).getActiveUserCount(testCompanyId);
        verify(subscriptionService).calculateMonthlyBill(testCompanyId);
    }

    @Test
    @DisplayName("generateInvoice - Deve lançar ResourceNotFoundException quando empresa não encontrada")
    void testGenerateInvoiceCompanyNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(companyRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> billingService.generateInvoice(nonExistentId, "2024-01"));

        verify(companyRepository).findById(nonExistentId);
        verify(authorizationService, never()).canAccessCompany(any());
    }

    @Test
    @DisplayName("generateInvoice - Deve lançar AccessDeniedException quando usuário não tem acesso")
    void testGenerateInvoiceAccessDenied() {
        when(companyRepository.findById(testCompanyId)).thenReturn(Optional.of(testCompany));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> billingService.generateInvoice(testCompanyId, "2024-01"));

        verify(companyRepository).findById(testCompanyId);
        verify(authorizationService).canAccessCompany(testCompanyId);
        verify(subscriptionRepository, never()).findByCompanyId(any());
    }

    @Test
    @DisplayName("generateInvoice - Deve lançar ResourceNotFoundException quando assinatura não encontrada")
    void testGenerateInvoiceSubscriptionNotFound() {
        when(companyRepository.findById(testCompanyId)).thenReturn(Optional.of(testCompany));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(true);
        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> billingService.generateInvoice(testCompanyId, "2024-01"));

        verify(subscriptionRepository).findByCompanyId(testCompanyId);
    }

    @Test
    @DisplayName("processPayment - Deve processar pagamento mensal com sucesso")
    void testProcessPaymentMonthlySuccess() {
        BigDecimal amount = new BigDecimal("1999.00");
        BigDecimal expectedAmount = new BigDecimal("1999.00");
        LocalDateTime now = LocalDateTime.now();

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(true);
        when(subscriptionService.calculateMonthlyBill(testCompanyId)).thenReturn(expectedAmount);
        when(subscriptionRepository.save(any(CompanySubscription.class))).thenReturn(testSubscription);

        boolean result = billingService.processPayment(testCompanyId, amount);

        assertTrue(result);
        assertEquals(SubscriptionStatus.ACTIVE, testSubscription.getStatus());
        assertNotNull(testSubscription.getNextBillingDate());
        assertTrue(testSubscription.getNextBillingDate().isAfter(now));

        verify(subscriptionRepository).findByCompanyId(testCompanyId);
        verify(authorizationService).canAccessCompany(testCompanyId);
        verify(subscriptionService).calculateMonthlyBill(testCompanyId);
        verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("processPayment - Deve processar pagamento anual com sucesso")
    void testProcessPaymentYearlySuccess() {
        testSubscription.setBillingCycle(BillingCycle.YEARLY);
        BigDecimal amount = new BigDecimal("19990.00");
        BigDecimal expectedAmount = new BigDecimal("19990.00");
        LocalDateTime now = LocalDateTime.now();

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(true);
        when(subscriptionService.calculateMonthlyBill(testCompanyId)).thenReturn(expectedAmount);
        when(subscriptionRepository.save(any(CompanySubscription.class))).thenReturn(testSubscription);

        boolean result = billingService.processPayment(testCompanyId, amount);

        assertTrue(result);
        assertEquals(SubscriptionStatus.ACTIVE, testSubscription.getStatus());
        assertNotNull(testSubscription.getNextBillingDate());
        assertTrue(testSubscription.getNextBillingDate().isAfter(now.plusMonths(11)));

        verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("processPayment - Deve lançar ResourceNotFoundException quando assinatura não encontrada")
    void testProcessPaymentSubscriptionNotFound() {
        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> billingService.processPayment(testCompanyId, new BigDecimal("1999.00")));

        verify(subscriptionRepository).findByCompanyId(testCompanyId);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("processPayment - Deve lançar AccessDeniedException quando usuário não tem acesso")
    void testProcessPaymentAccessDenied() {
        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> billingService.processPayment(testCompanyId, new BigDecimal("1999.00")));

        verify(authorizationService).canAccessCompany(testCompanyId);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("processPayment - Deve processar mesmo com valor menor que o esperado")
    void testProcessPaymentWithLowerAmount() {
        BigDecimal amount = new BigDecimal("1500.00");
        BigDecimal expectedAmount = new BigDecimal("1999.00");

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(true);
        when(subscriptionService.calculateMonthlyBill(testCompanyId)).thenReturn(expectedAmount);
        when(subscriptionRepository.save(any(CompanySubscription.class))).thenReturn(testSubscription);

        boolean result = billingService.processPayment(testCompanyId, amount);

        assertTrue(result);
        verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("processPaymentWithHistory - Deve processar pagamento com histórico")
    void testProcessPaymentWithHistorySuccess() {
        BigDecimal amount = new BigDecimal("1999.00");
        String stripeInvoiceId = "in_test123";

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(true);
        when(subscriptionService.calculateMonthlyBill(testCompanyId)).thenReturn(amount);
        when(subscriptionRepository.save(any(CompanySubscription.class))).thenReturn(testSubscription);

        boolean result = billingService.processPaymentWithHistory(testCompanyId, amount, stripeInvoiceId);

        assertTrue(result);
        verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("processPaymentWithHistory - Deve processar pagamento sem stripeInvoiceId")
    void testProcessPaymentWithHistoryWithoutInvoiceId() {
        BigDecimal amount = new BigDecimal("1999.00");

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(true);
        when(subscriptionService.calculateMonthlyBill(testCompanyId)).thenReturn(amount);
        when(subscriptionRepository.save(any(CompanySubscription.class))).thenReturn(testSubscription);

        boolean result = billingService.processPaymentWithHistory(testCompanyId, amount, null);

        assertTrue(result);
        verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("generateInvoice - Deve gerar fatura para período com ano bissexto")
    void testGenerateInvoiceLeapYear() {
        String period = "2024-02";
        int activeUsers = 10;
        BigDecimal totalAmount = new BigDecimal("1999.00");

        when(companyRepository.findById(testCompanyId)).thenReturn(Optional.of(testCompany));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(true);
        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(usageTrackingService.getActiveUserCount(testCompanyId)).thenReturn(activeUsers);
        when(subscriptionService.calculateMonthlyBill(testCompanyId)).thenReturn(totalAmount);

        BillingService.InvoiceDTO result = billingService.generateInvoice(testCompanyId, period);

        assertNotNull(result);
        assertEquals(LocalDate.of(2024, 2, 1), result.getStartDate());
        assertEquals(LocalDate.of(2024, 2, 29), result.getEndDate());
    }

    @Test
    @DisplayName("generateInvoice - Deve gerar fatura para mês com 31 dias")
    void testGenerateInvoice31DayMonth() {
        String period = "2024-01";
        int activeUsers = 10;
        BigDecimal totalAmount = new BigDecimal("1999.00");

        when(companyRepository.findById(testCompanyId)).thenReturn(Optional.of(testCompany));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(true);
        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(usageTrackingService.getActiveUserCount(testCompanyId)).thenReturn(activeUsers);
        when(subscriptionService.calculateMonthlyBill(testCompanyId)).thenReturn(totalAmount);

        BillingService.InvoiceDTO result = billingService.generateInvoice(testCompanyId, period);

        assertNotNull(result);
        assertEquals(LocalDate.of(2024, 1, 1), result.getStartDate());
        assertEquals(LocalDate.of(2024, 1, 31), result.getEndDate());
    }

    @Test
    @DisplayName("generateInvoice - Deve gerar fatura para mês com 30 dias")
    void testGenerateInvoice30DayMonth() {
        String period = "2024-04";
        int activeUsers = 10;
        BigDecimal totalAmount = new BigDecimal("1999.00");

        when(companyRepository.findById(testCompanyId)).thenReturn(Optional.of(testCompany));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(true);
        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(usageTrackingService.getActiveUserCount(testCompanyId)).thenReturn(activeUsers);
        when(subscriptionService.calculateMonthlyBill(testCompanyId)).thenReturn(totalAmount);

        BillingService.InvoiceDTO result = billingService.generateInvoice(testCompanyId, period);

        assertNotNull(result);
        assertEquals(LocalDate.of(2024, 4, 1), result.getStartDate());
        assertEquals(LocalDate.of(2024, 4, 30), result.getEndDate());
    }

    @Test
    @DisplayName("generateInvoice - Deve lançar exceção para período inválido")
    void testGenerateInvoiceInvalidPeriod() {
        when(companyRepository.findById(testCompanyId)).thenReturn(Optional.of(testCompany));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(true);
        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));

        assertThrows(Exception.class,
                () -> billingService.generateInvoice(testCompanyId, "invalid-period"));
    }

    @Test
    @DisplayName("processPayment - Deve processar pagamento com valor zero")
    void testProcessPaymentZeroAmount() {
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal expectedAmount = new BigDecimal("1999.00");

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(true);
        when(subscriptionService.calculateMonthlyBill(testCompanyId)).thenReturn(expectedAmount);
        when(subscriptionRepository.save(any(CompanySubscription.class))).thenReturn(testSubscription);

        boolean result = billingService.processPayment(testCompanyId, amount);

        assertTrue(result);
        verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("processPayment - Deve processar pagamento com valor maior que o esperado")
    void testProcessPaymentWithHigherAmount() {
        BigDecimal amount = new BigDecimal("2500.00");
        BigDecimal expectedAmount = new BigDecimal("1999.00");

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(true);
        when(subscriptionService.calculateMonthlyBill(testCompanyId)).thenReturn(expectedAmount);
        when(subscriptionRepository.save(any(CompanySubscription.class))).thenReturn(testSubscription);

        boolean result = billingService.processPayment(testCompanyId, amount);

        assertTrue(result);
        verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("processPaymentWithHistory - Deve processar pagamento com valor zero e histórico")
    void testProcessPaymentWithHistoryZeroAmount() {
        BigDecimal amount = BigDecimal.ZERO;
        String stripeInvoiceId = "in_test123";

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(authorizationService.canAccessCompany(testCompanyId)).thenReturn(true);
        when(subscriptionService.calculateMonthlyBill(testCompanyId)).thenReturn(amount);
        when(subscriptionRepository.save(any(CompanySubscription.class))).thenReturn(testSubscription);

        boolean result = billingService.processPaymentWithHistory(testCompanyId, amount, stripeInvoiceId);

        assertTrue(result);
        verify(subscriptionRepository).save(testSubscription);
    }
}
