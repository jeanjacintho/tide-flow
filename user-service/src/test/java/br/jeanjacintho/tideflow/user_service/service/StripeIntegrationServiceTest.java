package br.jeanjacintho.tideflow.user_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;

import br.jeanjacintho.tideflow.user_service.dto.request.CreateCheckoutSessionRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.CheckoutSessionResponseDTO;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.BillingCycle;
import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.CompanyStatus;
import br.jeanjacintho.tideflow.user_service.model.CompanySubscription;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionPlan;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionStatus;
import br.jeanjacintho.tideflow.user_service.repository.CompanyRepository;
import br.jeanjacintho.tideflow.user_service.repository.CompanySubscriptionRepository;
import br.jeanjacintho.tideflow.user_service.repository.PaymentHistoryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeIntegrationService Tests")
@SuppressWarnings("null")
class StripeIntegrationServiceTest {

    @Mock
    private StripeService stripeService;

    @Mock
    private CompanySubscriptionRepository subscriptionRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private UsageTrackingService usageTrackingService;

    @Mock
    private PaymentHistoryService paymentHistoryService;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private StripeIntegrationService stripeIntegrationService;

    private Company testCompany;
    private CompanySubscription testSubscription;
    private UUID testCompanyId;
    private String testCustomerId;
    private String testSubscriptionId;
    private String testPriceId;

    @BeforeEach
    void setUp() {
        testCompanyId = UUID.randomUUID();
        testCustomerId = "cus_test123";
        testSubscriptionId = "sub_test123";
        testPriceId = "price_test123";
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
        testSubscription.setStripeCustomerId(testCustomerId);
        testSubscription.setStripeSubscriptionId(testSubscriptionId);
    }

    @Test
    @DisplayName("startCheckoutSession - Deve criar sessão de checkout com sucesso")
    void testStartCheckoutSessionSuccess() throws StripeException {
        String checkoutUrl = "https://checkout.stripe.com/test";
        CreateCheckoutSessionRequestDTO request = new CreateCheckoutSessionRequestDTO(
                testCompanyId, 
                "https://example.com/success", 
                "https://example.com/cancel"
        );

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(stripeService.getOrCreateEnterprisePrice()).thenReturn(testPriceId);
        when(stripeService.createCheckoutSession(
                eq(testCompanyId), 
                eq(testCustomerId), 
                eq(testPriceId), 
                anyString(), 
                anyString()
        )).thenReturn(checkoutUrl);

        CheckoutSessionResponseDTO result = stripeIntegrationService.startCheckoutSession(request);

        assertNotNull(result);
        assertEquals(checkoutUrl, result.checkoutUrl());
        verify(subscriptionRepository).findByCompanyId(testCompanyId);
        verify(stripeService).getOrCreateEnterprisePrice();
        verify(stripeService).createCheckoutSession(
                eq(testCompanyId), 
                eq(testCustomerId), 
                eq(testPriceId), 
                anyString(), 
                anyString()
        );
    }

    @Test
    @DisplayName("startCheckoutSession - Deve criar assinatura FREE se não existir")
    void testStartCheckoutSessionCreatesFreeSubscription() throws StripeException {
        String checkoutUrl = "https://checkout.stripe.com/test";
        CreateCheckoutSessionRequestDTO request = new CreateCheckoutSessionRequestDTO(
                testCompanyId, 
                null, 
                null
        );

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.empty());
        when(companyRepository.findById(testCompanyId)).thenReturn(Optional.of(testCompany));
        when(usageTrackingService.getActiveUserCount(testCompanyId)).thenReturn(5);
        when(subscriptionRepository.save(any(CompanySubscription.class))).thenReturn(testSubscription);
        lenient().when(stripeService.getOrCreateCustomer(any(), any(), any())).thenReturn(testCustomerId);
        when(stripeService.getOrCreateEnterprisePrice()).thenReturn(testPriceId);
        when(stripeService.createCheckoutSession(any(), any(), any(), any(), any())).thenReturn(checkoutUrl);

        CheckoutSessionResponseDTO result = stripeIntegrationService.startCheckoutSession(request);

        assertNotNull(result);
        assertEquals(checkoutUrl, result.checkoutUrl());
        verify(subscriptionRepository).findByCompanyId(testCompanyId);
        verify(subscriptionRepository).save(any(CompanySubscription.class));
    }

    @Test
    @DisplayName("startCheckoutSession - Deve lançar RuntimeException quando Stripe falha")
    void testStartCheckoutSessionStripeException() {
        CreateCheckoutSessionRequestDTO request = new CreateCheckoutSessionRequestDTO(
                testCompanyId, 
                null, 
                null
        );

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        try {
            doThrow(new RuntimeException("Stripe error")).when(stripeService).getOrCreateEnterprisePrice();
        } catch (StripeException e) {
            // Won't happen in test
        }

        assertThrows(RuntimeException.class, 
                () -> stripeIntegrationService.startCheckoutSession(request));
    }

    @Test
    @DisplayName("cancelSubscription - Deve cancelar assinatura com sucesso")
    void testCancelSubscriptionSuccess() throws StripeException {
        Subscription cancelledSubscription = new Subscription();
        cancelledSubscription.setId(testSubscriptionId);
        cancelledSubscription.setStatus("canceled");

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(stripeService.cancelSubscription(testSubscriptionId)).thenReturn(cancelledSubscription);
        doNothing().when(subscriptionService).cancelStripeSubscription(testCustomerId);

        stripeIntegrationService.cancelSubscription(testCompanyId);

        verify(subscriptionRepository).findByCompanyId(testCompanyId);
        verify(stripeService).cancelSubscription(testSubscriptionId);
        verify(subscriptionService).cancelStripeSubscription(testCustomerId);
    }

    @Test
    @DisplayName("cancelSubscription - Deve cancelar mesmo sem stripeSubscriptionId")
    void testCancelSubscriptionWithoutStripeId() {
        testSubscription.setStripeSubscriptionId(null);

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        doNothing().when(subscriptionService).cancelStripeSubscription(testCustomerId);

        stripeIntegrationService.cancelSubscription(testCompanyId);

        verify(subscriptionRepository).findByCompanyId(testCompanyId);
        verify(subscriptionService).cancelStripeSubscription(testCustomerId);
    }

    @Test
    @DisplayName("cancelSubscription - Deve lançar ResourceNotFoundException quando assinatura não encontrada")
    void testCancelSubscriptionNotFound() {
        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> stripeIntegrationService.cancelSubscription(testCompanyId));
    }

    @Test
    @DisplayName("forceSyncWithStripe - Deve sincronizar com sucesso")
    void testForceSyncWithStripeSuccess() throws StripeException {
        Subscription stripeSubscription = new Subscription();
        stripeSubscription.setId(testSubscriptionId);
        stripeSubscription.setCustomer(testCustomerId);
        stripeSubscription.setStatus("active");

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(stripeService.getSubscription(testSubscriptionId)).thenReturn(stripeSubscription);
        doNothing().when(subscriptionService).syncSubscriptionFromStripe(anyString(), any(Subscription.class));

        Map<String, Object> result = stripeIntegrationService.forceSyncWithStripe(testCompanyId);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertEquals(testSubscriptionId, result.get("stripeSubscriptionId"));
        verify(stripeService).getSubscription(testSubscriptionId);
        verify(subscriptionService).syncSubscriptionFromStripe(eq(testCustomerId), eq(stripeSubscription));
    }

    @Test
    @DisplayName("forceSyncWithStripe - Deve lançar IllegalStateException sem stripeSubscriptionId")
    void testForceSyncWithStripeNoSubscriptionId() {
        testSubscription.setStripeSubscriptionId(null);

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));

        assertThrows(IllegalStateException.class, 
                () -> stripeIntegrationService.forceSyncWithStripe(testCompanyId));
    }

    @Test
    @DisplayName("forceSyncWithStripe - Deve lançar IllegalStateException sem stripeCustomerId")
    void testForceSyncWithStripeNoCustomerId() {
        testSubscription.setStripeCustomerId(null);

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));

        assertThrows(IllegalStateException.class, 
                () -> stripeIntegrationService.forceSyncWithStripe(testCompanyId));
    }

    @Test
    @DisplayName("testSubscriptionLookup - Deve encontrar assinatura por customerId")
    void testSubscriptionLookupByCustomerId() {
        when(subscriptionRepository.findByStripeCustomerId(testCustomerId)).thenReturn(Optional.of(testSubscription));

        Map<String, Object> result = stripeIntegrationService.testSubscriptionLookup(
                null, testCustomerId, null);

        assertNotNull(result);
        assertTrue((Boolean) result.get("found"));
        assertEquals(testCompanyId.toString(), result.get("companyId"));
    }

    @Test
    @DisplayName("testSubscriptionLookup - Deve encontrar assinatura por subscriptionId")
    void testSubscriptionLookupBySubscriptionId() {
        when(subscriptionRepository.findByStripeCustomerId(testCustomerId)).thenReturn(Optional.empty());
        when(subscriptionRepository.findByStripeSubscriptionId(testSubscriptionId)).thenReturn(Optional.of(testSubscription));

        Map<String, Object> result = stripeIntegrationService.testSubscriptionLookup(
                testSubscriptionId, testCustomerId, null);

        assertNotNull(result);
        assertTrue((Boolean) result.get("found"));
    }

    @Test
    @DisplayName("testSubscriptionLookup - Deve retornar not found quando não encontra")
    void testSubscriptionLookupNotFound() {
        when(subscriptionRepository.findByStripeCustomerId(testCustomerId)).thenReturn(Optional.empty());
        when(subscriptionRepository.findByStripeSubscriptionId(testSubscriptionId)).thenReturn(Optional.empty());
        try {
            when(stripeService.getSubscription(testSubscriptionId)).thenAnswer(invocation -> {
                throw new RuntimeException("Subscription not found");
            });
        } catch (StripeException e) {
            // Won't happen in test setup
        }

        Map<String, Object> result = stripeIntegrationService.testSubscriptionLookup(
                testSubscriptionId, testCustomerId, null);

        assertNotNull(result);
        assertEquals(false, result.get("found"));
    }

    @Test
    @DisplayName("checkStripeMetadata - Deve retornar metadados com sucesso")
    void testCheckStripeMetadataSuccess() throws StripeException {
        Subscription stripeSubscription = new Subscription();
        stripeSubscription.setId(testSubscriptionId);
        stripeSubscription.setCustomer(testCustomerId);
        stripeSubscription.setStatus("active");
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("company_id", testCompanyId.toString());
        stripeSubscription.setMetadata(metadata);

        when(stripeService.getSubscription(testSubscriptionId)).thenReturn(stripeSubscription);

        Map<String, Object> result = stripeIntegrationService.checkStripeMetadata(testSubscriptionId);

        assertNotNull(result);
        assertEquals(testSubscriptionId, result.get("subscriptionId"));
        assertEquals(true, result.get("hasCompanyId"));
        assertEquals(testCompanyId.toString(), result.get("companyId"));
        assertEquals(testCustomerId, result.get("stripeCustomerId"));
    }

    @Test
    @DisplayName("checkStripeMetadata - Deve lançar RuntimeException quando Stripe falha")
    void testCheckStripeMetadataStripeException() throws StripeException {
        doThrow(new RuntimeException("Stripe error")).when(stripeService).getSubscription(testSubscriptionId);

        assertThrows(RuntimeException.class, 
                () -> stripeIntegrationService.checkStripeMetadata(testSubscriptionId));
    }

    @Test
    @DisplayName("processWebhook - Deve lançar IllegalStateException sem webhook secret")
    void testProcessWebhookNoSecret() {
        when(stripeService.getWebhookSecret()).thenReturn(null);

        assertThrows(IllegalStateException.class, 
                () -> stripeIntegrationService.processWebhook("payload", "signature"));
    }

    @Test
    @DisplayName("processWebhook - Deve lançar IllegalArgumentException sem signature")
    void testProcessWebhookNoSignature() {
        when(stripeService.getWebhookSecret()).thenReturn("whsec_test");

        assertThrows(IllegalArgumentException.class, 
                () -> stripeIntegrationService.processWebhook("payload", null));
    }

    @Test
    @DisplayName("processWebhook - Deve lançar RuntimeException com payload inválido")
    void testProcessWebhookInvalidSignature() {
        when(stripeService.getWebhookSecret()).thenReturn("whsec_test");

        // Simula exceção de parsing JSON que é capturada e relançada como RuntimeException
        assertThrows(RuntimeException.class, 
                () -> stripeIntegrationService.processWebhook("invalid_payload", "invalid_signature"));
    }

    @Test
    @DisplayName("cancelSubscription - Deve lançar RuntimeException quando StripeException ocorre")
    void testCancelSubscriptionStripeException() throws StripeException {
        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        doThrow(new RuntimeException("Stripe API error"))
                .when(stripeService).cancelSubscription(testSubscriptionId);

        assertThrows(RuntimeException.class, 
                () -> stripeIntegrationService.cancelSubscription(testCompanyId));
        
        verify(stripeService).cancelSubscription(testSubscriptionId);
    }

    @Test
    @DisplayName("cancelSubscription - Deve cancelar localmente quando não há stripeCustomerId")
    void testCancelSubscriptionNoCustomerId() {
        testSubscription.setStripeSubscriptionId(null);
        testSubscription.setStripeCustomerId(null);

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(CompanySubscription.class))).thenReturn(testSubscription);
        when(companyRepository.save(any(Company.class))).thenReturn(testCompany);

        stripeIntegrationService.cancelSubscription(testCompanyId);

        verify(subscriptionRepository).findByCompanyId(testCompanyId);
        verify(subscriptionRepository).save(testSubscription);
        verify(companyRepository).save(testCompany);
        assertEquals(SubscriptionStatus.CANCELLED, testSubscription.getStatus());
        assertEquals(SubscriptionPlan.FREE, testSubscription.getPlanType());
    }

    @Test
    @DisplayName("forceSyncWithStripe - Deve lançar RuntimeException quando syncSubscriptionFromStripe falha")
    void testForceSyncWithStripeSyncFails() throws StripeException {
        Subscription stripeSubscription = new Subscription();
        stripeSubscription.setId(testSubscriptionId);
        stripeSubscription.setCustomer(testCustomerId);
        stripeSubscription.setStatus("active");

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(stripeService.getSubscription(testSubscriptionId)).thenReturn(stripeSubscription);
        doThrow(new RuntimeException("Sync failed"))
                .when(subscriptionService).syncSubscriptionFromStripe(anyString(), any(Subscription.class));

        assertThrows(RuntimeException.class, 
                () -> stripeIntegrationService.forceSyncWithStripe(testCompanyId));
        
        verify(stripeService).getSubscription(testSubscriptionId);
        verify(subscriptionService).syncSubscriptionFromStripe(eq(testCustomerId), eq(stripeSubscription));
    }

    @Test
    @DisplayName("forceSyncWithStripe - Deve lançar RuntimeException quando getSubscription falha")
    void testForceSyncWithStripeGetSubscriptionFails() throws StripeException {
        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        doThrow(new RuntimeException("Subscription not found"))
                .when(stripeService).getSubscription(testSubscriptionId);

        assertThrows(RuntimeException.class, 
                () -> stripeIntegrationService.forceSyncWithStripe(testCompanyId));
        
        verify(stripeService).getSubscription(testSubscriptionId);
    }

    @Test
    @DisplayName("checkStripeMetadata - Deve retornar metadados vazios quando não há metadata")
    void testCheckStripeMetadataNoMetadata() throws StripeException {
        Subscription stripeSubscription = new Subscription();
        stripeSubscription.setId(testSubscriptionId);
        stripeSubscription.setCustomer(testCustomerId);
        stripeSubscription.setStatus("active");
        stripeSubscription.setMetadata(null);

        when(stripeService.getSubscription(testSubscriptionId)).thenReturn(stripeSubscription);

        Map<String, Object> result = stripeIntegrationService.checkStripeMetadata(testSubscriptionId);

        assertNotNull(result);
        assertEquals(testSubscriptionId, result.get("subscriptionId"));
        assertEquals(false, result.get("hasCompanyId"));
        assertNotNull(result.get("metadata"));
    }

    @Test
    @DisplayName("testSubscriptionLookup - Deve encontrar assinatura por invoiceId através de metadata")
    void testSubscriptionLookupByInvoiceIdMetadata() throws StripeException {
        Subscription stripeSubscription = new Subscription();
        stripeSubscription.setId(testSubscriptionId);
        stripeSubscription.setCustomer(testCustomerId);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("company_id", testCompanyId.toString());
        stripeSubscription.setMetadata(metadata);

        when(subscriptionRepository.findByStripeCustomerId(testCustomerId)).thenReturn(Optional.empty());
        when(subscriptionRepository.findByStripeSubscriptionId(testSubscriptionId)).thenReturn(Optional.empty());
        when(stripeService.getSubscription(testSubscriptionId)).thenReturn(stripeSubscription);
        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));

        Map<String, Object> result = stripeIntegrationService.testSubscriptionLookup(
                testSubscriptionId, testCustomerId, "in_test123");

        assertNotNull(result);
        assertTrue((Boolean) result.get("found"));
    }

    @Test
    @DisplayName("startCheckoutSession - Deve usar URLs padrão quando não fornecidas")
    void testStartCheckoutSessionDefaultUrls() throws StripeException {
        CreateCheckoutSessionRequestDTO request = new CreateCheckoutSessionRequestDTO(
                testCompanyId, null, null);

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(stripeService.getOrCreateEnterprisePrice()).thenReturn(testPriceId);
        when(stripeService.createCheckoutSession(
                eq(testCompanyId), 
                eq(testCustomerId), 
                eq(testPriceId), 
                anyString(), 
                anyString()))
                .thenReturn("https://checkout.stripe.com/test");

        CheckoutSessionResponseDTO result = stripeIntegrationService.startCheckoutSession(request);

        assertNotNull(result);
        assertNotNull(result.checkoutUrl());
        verify(stripeService).getOrCreateEnterprisePrice();
        verify(stripeService).createCheckoutSession(
                eq(testCompanyId),
                eq(testCustomerId),
                eq(testPriceId),
                anyString(),
                anyString());
    }

    @Test
    @DisplayName("startCheckoutSession - Deve usar URLs customizadas quando fornecidas")
    void testStartCheckoutSessionCustomUrls() throws StripeException {
        String customSuccessUrl = "https://custom.com/success";
        String customCancelUrl = "https://custom.com/cancel";
        CreateCheckoutSessionRequestDTO request = new CreateCheckoutSessionRequestDTO(
                testCompanyId, customSuccessUrl, customCancelUrl);

        when(subscriptionRepository.findByCompanyId(testCompanyId)).thenReturn(Optional.of(testSubscription));
        when(stripeService.getOrCreateEnterprisePrice()).thenReturn(testPriceId);
        when(stripeService.createCheckoutSession(
                eq(testCompanyId), 
                eq(testCustomerId), 
                eq(testPriceId), 
                eq(customSuccessUrl), 
                eq(customCancelUrl)))
                .thenReturn("https://checkout.stripe.com/test");

        CheckoutSessionResponseDTO result = stripeIntegrationService.startCheckoutSession(request);

        assertNotNull(result);
        verify(stripeService).getOrCreateEnterprisePrice();
        verify(stripeService).createCheckoutSession(
                eq(testCompanyId),
                eq(testCustomerId),
                eq(testPriceId),
                eq(customSuccessUrl),
                eq(customCancelUrl));
    }
}
