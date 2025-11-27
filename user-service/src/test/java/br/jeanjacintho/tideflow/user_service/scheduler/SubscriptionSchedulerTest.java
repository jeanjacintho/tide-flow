package br.jeanjacintho.tideflow.user_service.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.jeanjacintho.tideflow.user_service.model.BillingCycle;
import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.CompanyStatus;
import br.jeanjacintho.tideflow.user_service.model.CompanySubscription;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionPlan;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionStatus;
import br.jeanjacintho.tideflow.user_service.repository.CompanySubscriptionRepository;
import br.jeanjacintho.tideflow.user_service.service.SubscriptionService;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionScheduler Tests")
@SuppressWarnings("null")
class SubscriptionSchedulerTest {

    @Mock
    private CompanySubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionScheduler subscriptionScheduler;

    private Company testCompany;
    private CompanySubscription expiredTrialSubscription;
    private CompanySubscription expiredActiveSubscription;
    private CompanySubscription activeSubscription;
    private UUID testCompanyId;

    @BeforeEach
    void setUp() {
        testCompanyId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusDays(1);

        testCompany = new Company();
        testCompany.setId(testCompanyId);
        testCompany.setName("Test Company");
        testCompany.setDomain("test.com");
        testCompany.setSubscriptionPlan(SubscriptionPlan.ENTERPRISE);
        testCompany.setMaxEmployees(100);
        testCompany.setStatus(CompanyStatus.ACTIVE);

        expiredTrialSubscription = new CompanySubscription();
        expiredTrialSubscription.setId(UUID.randomUUID());
        expiredTrialSubscription.setCompany(testCompany);
        expiredTrialSubscription.setPlanType(SubscriptionPlan.ENTERPRISE);
        expiredTrialSubscription.setPricePerUser(new BigDecimal("199.90"));
        expiredTrialSubscription.setTotalUsers(10);
        expiredTrialSubscription.setBillingCycle(BillingCycle.MONTHLY);
        expiredTrialSubscription.setStatus(SubscriptionStatus.TRIAL);
        expiredTrialSubscription.setNextBillingDate(past);

        expiredActiveSubscription = new CompanySubscription();
        expiredActiveSubscription.setId(UUID.randomUUID());
        expiredActiveSubscription.setCompany(testCompany);
        expiredActiveSubscription.setPlanType(SubscriptionPlan.ENTERPRISE);
        expiredActiveSubscription.setPricePerUser(new BigDecimal("199.90"));
        expiredActiveSubscription.setTotalUsers(10);
        expiredActiveSubscription.setBillingCycle(BillingCycle.MONTHLY);
        expiredActiveSubscription.setStatus(SubscriptionStatus.ACTIVE);
        expiredActiveSubscription.setNextBillingDate(past);

        activeSubscription = new CompanySubscription();
        activeSubscription.setId(UUID.randomUUID());
        activeSubscription.setCompany(testCompany);
        activeSubscription.setPlanType(SubscriptionPlan.ENTERPRISE);
        activeSubscription.setPricePerUser(new BigDecimal("199.90"));
        activeSubscription.setTotalUsers(10);
        activeSubscription.setBillingCycle(BillingCycle.MONTHLY);
        activeSubscription.setStatus(SubscriptionStatus.ACTIVE);
        activeSubscription.setNextBillingDate(now.plusMonths(1));
    }

    @Test
    @DisplayName("checkExpiredSubscriptions - Deve suspender assinaturas TRIAL expiradas")
    void testCheckExpiredSubscriptionsSuspendsExpiredTrials() {
        List<CompanySubscription> expiredTrials = Arrays.asList(expiredTrialSubscription);

        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.TRIAL)))
                .thenReturn(expiredTrials);
        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        subscriptionScheduler.checkExpiredSubscriptions();

        verify(subscriptionRepository).findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.TRIAL));
        verify(subscriptionService).suspendSubscription(testCompanyId);
    }

    @Test
    @DisplayName("checkExpiredSubscriptions - Deve suspender assinaturas ACTIVE expiradas")
    void testCheckExpiredSubscriptionsSuspendsExpiredActive() {
        List<CompanySubscription> expiredActive = Arrays.asList(expiredActiveSubscription);

        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.TRIAL)))
                .thenReturn(Collections.emptyList());
        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(expiredActive);

        subscriptionScheduler.checkExpiredSubscriptions();

        verify(subscriptionRepository).findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.ACTIVE));
        verify(subscriptionService).suspendSubscription(testCompanyId);
    }

    @Test
    @DisplayName("checkExpiredSubscriptions - Não deve suspender assinaturas ativas")
    void testCheckExpiredSubscriptionsDoesNotSuspendActive() {
        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.TRIAL)))
                .thenReturn(Collections.emptyList());
        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        subscriptionScheduler.checkExpiredSubscriptions();

        verify(subscriptionRepository).findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.TRIAL));
        verify(subscriptionRepository).findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.ACTIVE));
        verify(subscriptionService, never()).suspendSubscription(any(UUID.class));
    }

    @Test
    @DisplayName("checkExpiredSubscriptions - Deve processar múltiplas assinaturas expiradas")
    void testCheckExpiredSubscriptionsMultipleExpired() {
        LocalDateTime now = LocalDateTime.now();
        
        UUID anotherCompanyId = UUID.randomUUID();
        Company anotherCompany = new Company();
        anotherCompany.setId(anotherCompanyId);
        anotherCompany.setName("Another Company");
        
        CompanySubscription anotherExpiredTrial = new CompanySubscription();
        anotherExpiredTrial.setId(UUID.randomUUID());
        anotherExpiredTrial.setCompany(anotherCompany);
        anotherExpiredTrial.setStatus(SubscriptionStatus.TRIAL);
        anotherExpiredTrial.setNextBillingDate(now.minusDays(2));

        CompanySubscription anotherExpiredActive = new CompanySubscription();
        anotherExpiredActive.setId(UUID.randomUUID());
        anotherExpiredActive.setCompany(anotherCompany);
        anotherExpiredActive.setStatus(SubscriptionStatus.ACTIVE);
        anotherExpiredActive.setNextBillingDate(now.minusDays(1));

        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.TRIAL)))
                .thenReturn(Arrays.asList(expiredTrialSubscription, anotherExpiredTrial));
        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(Arrays.asList(expiredActiveSubscription, anotherExpiredActive));

        subscriptionScheduler.checkExpiredSubscriptions();

        verify(subscriptionService, Mockito.times(2)).suspendSubscription(testCompanyId);
        verify(subscriptionService, Mockito.times(2)).suspendSubscription(anotherCompanyId);
    }

    @Test
    @DisplayName("checkExpiredSubscriptions - Deve ignorar assinaturas com company null")
    void testCheckExpiredSubscriptionsIgnoresNullCompany() {
        LocalDateTime now = LocalDateTime.now();
        
        CompanySubscription subscriptionWithNullCompany = new CompanySubscription();
        subscriptionWithNullCompany.setId(UUID.randomUUID());
        subscriptionWithNullCompany.setCompany(null);
        subscriptionWithNullCompany.setStatus(SubscriptionStatus.TRIAL);
        subscriptionWithNullCompany.setNextBillingDate(now.minusDays(1));

        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.TRIAL)))
                .thenReturn(Arrays.asList(subscriptionWithNullCompany));
        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        subscriptionScheduler.checkExpiredSubscriptions();

        verify(subscriptionService, never()).suspendSubscription(any(UUID.class));
    }

    @Test
    @DisplayName("checkExpiredSubscriptions - Deve ignorar assinaturas com companyId null")
    void testCheckExpiredSubscriptionsIgnoresNullCompanyId() {
        LocalDateTime now = LocalDateTime.now();
        
        Company companyWithNullId = new Company();
        companyWithNullId.setId(null);
        companyWithNullId.setName("Company Without ID");

        CompanySubscription subscriptionWithNullCompanyId = new CompanySubscription();
        subscriptionWithNullCompanyId.setId(UUID.randomUUID());
        subscriptionWithNullCompanyId.setCompany(companyWithNullId);
        subscriptionWithNullCompanyId.setStatus(SubscriptionStatus.TRIAL);
        subscriptionWithNullCompanyId.setNextBillingDate(now.minusDays(1));

        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.TRIAL)))
                .thenReturn(Arrays.asList(subscriptionWithNullCompanyId));
        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        subscriptionScheduler.checkExpiredSubscriptions();

        verify(subscriptionService, never()).suspendSubscription(any(UUID.class));
    }

    @Test
    @DisplayName("checkExpiredSubscriptions - Deve processar TRIAL e ACTIVE expiradas para mesma empresa")
    void testCheckExpiredSubscriptionsBothStatusesSameCompany() {
        List<CompanySubscription> expiredTrials = Arrays.asList(expiredTrialSubscription);
        List<CompanySubscription> expiredActive = Arrays.asList(expiredActiveSubscription);

        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.TRIAL)))
                .thenReturn(expiredTrials);
        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(expiredActive);

        subscriptionScheduler.checkExpiredSubscriptions();

        verify(subscriptionService, Mockito.times(2)).suspendSubscription(testCompanyId);
    }

    @Test
    @DisplayName("checkExpiredSubscriptions - Deve lidar com listas vazias corretamente")
    void testCheckExpiredSubscriptionsEmptyLists() {
        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.TRIAL)))
                .thenReturn(Collections.emptyList());
        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        subscriptionScheduler.checkExpiredSubscriptions();

        verify(subscriptionRepository).findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.TRIAL));
        verify(subscriptionRepository).findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.ACTIVE));
        verify(subscriptionService, never()).suspendSubscription(any(UUID.class));
    }

    @Test
    @DisplayName("checkExpiredSubscriptions - Deve processar assinaturas expiradas exatamente agora")
    void testCheckExpiredSubscriptionsExpiredExactlyNow() {
        LocalDateTime now = LocalDateTime.now();
        
        CompanySubscription exactlyExpiredTrial = new CompanySubscription();
        exactlyExpiredTrial.setId(UUID.randomUUID());
        exactlyExpiredTrial.setCompany(testCompany);
        exactlyExpiredTrial.setStatus(SubscriptionStatus.TRIAL);
        exactlyExpiredTrial.setNextBillingDate(now.minusMinutes(1));

        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.TRIAL)))
                .thenReturn(Arrays.asList(exactlyExpiredTrial));
        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        subscriptionScheduler.checkExpiredSubscriptions();

        verify(subscriptionService).suspendSubscription(testCompanyId);
    }

    @Test
    @DisplayName("checkExpiredSubscriptions - Deve processar múltiplas assinaturas com company null misturadas")
    void testCheckExpiredSubscriptionsMixedNullCompanies() {
        LocalDateTime now = LocalDateTime.now();
        
        CompanySubscription subscriptionWithNullCompany = new CompanySubscription();
        subscriptionWithNullCompany.setId(UUID.randomUUID());
        subscriptionWithNullCompany.setCompany(null);
        subscriptionWithNullCompany.setStatus(SubscriptionStatus.TRIAL);
        subscriptionWithNullCompany.setNextBillingDate(now.minusDays(1));

        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.TRIAL)))
                .thenReturn(Arrays.asList(expiredTrialSubscription, subscriptionWithNullCompany));
        when(subscriptionRepository.findExpiredSubscriptions(any(LocalDateTime.class), eq(SubscriptionStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        subscriptionScheduler.checkExpiredSubscriptions();

        verify(subscriptionService).suspendSubscription(testCompanyId);
    }
}
