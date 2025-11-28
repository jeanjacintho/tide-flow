package br.jeanjacintho.tideflow.user_service.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.jeanjacintho.tideflow.user_service.model.CompanySubscription;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionStatus;
import br.jeanjacintho.tideflow.user_service.repository.CompanySubscriptionRepository;
import br.jeanjacintho.tideflow.user_service.service.SubscriptionService;

@Component
public class SubscriptionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionScheduler.class);

    private final CompanySubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    public SubscriptionScheduler(CompanySubscriptionRepository subscriptionRepository, SubscriptionService subscriptionService) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    @SuppressWarnings("null")
    public void checkExpiredSubscriptions() {
        logger.info("Checking for expired subscriptions...");

        LocalDateTime now = LocalDateTime.now();

        List<CompanySubscription> expiredTrials = subscriptionRepository.findExpiredSubscriptions(now, SubscriptionStatus.TRIAL);
        for (CompanySubscription sub : expiredTrials) {
            if (sub.getCompany() != null && sub.getCompany().getId() != null) {
                UUID companyId = sub.getCompany().getId();
                logger.info("Trial expired for company: {}", companyId);
                subscriptionService.suspendSubscription(companyId);
            } else {
                logger.warn("Subscription {} has null company or company ID, skipping", sub.getId());
            }
        }

        List<CompanySubscription> expiredActive = subscriptionRepository.findExpiredSubscriptions(now, SubscriptionStatus.ACTIVE);
        for (CompanySubscription sub : expiredActive) {
            if (sub.getCompany() != null && sub.getCompany().getId() != null) {
                UUID companyId = sub.getCompany().getId();
                logger.info("Active subscription expired for company: {}", companyId);

                subscriptionService.suspendSubscription(companyId);
            } else {
                logger.warn("Subscription {} has null company or company ID, skipping", sub.getId());
            }
        }
    }
}
