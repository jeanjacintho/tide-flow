package br.jeanjacintho.tideflow.user_service.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerCollection;
import com.stripe.model.Price;
import com.stripe.model.PriceCollection;
import com.stripe.model.Product;
import com.stripe.model.ProductCollection;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.ProductListParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    @Value("${STRIPE_SECRET_KEY:}")
    private String stripeSecretKey;

    @Value("${STRIPE_PUBLISHABLE_KEY:}")
    private String stripePublishableKey;

    @Value("${STRIPE_WEBHOOK_SECRET:}")
    private String webhookSecret;

    @Value("${STRIPE_ENTERPRISE_PRICE_ID:}")
    private String enterprisePriceId;

    @Value("${FRONTEND_URL:http://localhost:3000}")
    public String frontendUrl;

    @Value("${tideflow.subscription.price-brl:19990}")
    private Long subscriptionPrice;

    @Value("${tideflow.subscription.currency:brl}")
    private String subscriptionCurrency;

    @Value("${tideflow.subscription.duration-minutes:5}")
    private int subscriptionDurationMinutes;

    @PostConstruct
    public void init() {
        if (stripeSecretKey != null && !stripeSecretKey.isEmpty()) {
            Stripe.apiKey = stripeSecretKey;
            logger.info("Stripe initialized with API key");
        } else {
            logger.warn("STRIPE_SECRET_KEY not found in environment variables");
        }
    }

    public String getOrCreateCustomer(UUID companyId, String companyName, String email) throws StripeException {
        logger.info("Getting or creating Stripe customer for company: {} with email: {}", companyId, email);

        try {
            CustomerListParams searchParams = CustomerListParams.builder()
                    .addExpand("data.metadata")
                    .build();

            CustomerCollection customers = Customer.list(searchParams);

            for (Customer existingCustomer : customers.getData()) {
                Map<String, String> metadata = existingCustomer.getMetadata();
                if (metadata != null && companyId.toString().equals(metadata.get("company_id"))) {
                    logger.info("Found existing Stripe customer: {} for company: {}", existingCustomer.getId(), companyId);
                    return existingCustomer.getId();
                }
            }
        } catch (StripeException e) {
            logger.warn("Error searching for existing customer, will create new one: {}", e.getMessage());
        }

        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(companyName != null && !companyName.trim().isEmpty() ? companyName : "Company " + companyId.toString().substring(0, 8))
                .putMetadata("company_id", companyId.toString())
                .build();

        Customer customer = Customer.create(customerParams);
        logger.info("Created new Stripe customer: {} for company: {}", customer.getId(), companyId);

        return customer.getId();
    }

    public String getOrCreateEnterprisePrice() throws StripeException {
        if (enterprisePriceId != null && !enterprisePriceId.isEmpty()) {
            try {
                Price.retrieve(enterprisePriceId);
                logger.info("Using configured Stripe price: {}", enterprisePriceId);
                return enterprisePriceId;
            } catch (StripeException e) {
                logger.warn("Configured price ID not found, will search or create new price: {}", e.getMessage());
            }
        }

        String productName = "TideFlow Enterprise Plan";
        Product existingProduct = null;
        try {
            ProductListParams productListParams = ProductListParams.builder()
                    .setLimit(100L)
                    .build();
            ProductCollection products = Product.list(productListParams);

            for (Product product : products.getData()) {
                if (productName.equals(product.getName())) {
                    existingProduct = product;
                    logger.info("Found existing Stripe product: {}", product.getId());
                    break;
                }
            }
        } catch (StripeException e) {
            logger.warn("Error searching for existing product, will create new one: {}", e.getMessage());
        }

        Product product = existingProduct;
        if (product == null) {
            ProductCreateParams productParams = ProductCreateParams.builder()
                    .setName(productName)
                    .setDescription("Plano Enterprise - Usuários ilimitados e recursos avançados")
                    .putMetadata("plan_type", "ENTERPRISE")
                    .build();

            product = Product.create(productParams);
            logger.info("Created new Stripe product: {}", product.getId());
        }

        Price existingPrice = null;
        try {
            PriceListParams priceListParams = PriceListParams.builder()
                    .setProduct(product.getId())
                    .setLimit(100L)
                    .build();
            PriceCollection prices = Price.list(priceListParams);

            for (Price price : prices.getData()) {
                if (subscriptionCurrency.equalsIgnoreCase(price.getCurrency())
                        && price.getUnitAmount() != null
                        && price.getUnitAmount().equals(subscriptionPrice)
                        && price.getRecurring() != null
                        && "month".equals(price.getRecurring().getInterval())) {
                    existingPrice = price;
                    logger.info("Found existing Stripe price: {}", price.getId());
                    break;
                }
            }
        } catch (StripeException e) {
            logger.warn("Error searching for existing price, will create new one: {}", e.getMessage());
        }

        if (existingPrice != null) {
            return existingPrice.getId();
        }

        PriceCreateParams priceParams = PriceCreateParams.builder()
                .setProduct(product.getId())
                .setCurrency(subscriptionCurrency)
                .setUnitAmount(subscriptionPrice)
                .setRecurring(PriceCreateParams.Recurring.builder()
                        .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                        .build())
                .putMetadata("plan_type", "ENTERPRISE")
                .build();

        Price price = Price.create(priceParams);
        logger.info("Created new Stripe price: {} ({} {})", price.getId(), subscriptionCurrency, subscriptionPrice);

        return price.getId();
    }

    public String createCheckoutSession(UUID companyId, String customerId, String priceId,
                                       String successUrl, String cancelUrl) throws StripeException {
        logger.info("Creating checkout session for company: {}", companyId);

        SessionCreateParams.SubscriptionData.Builder subscriptionDataBuilder =
                SessionCreateParams.SubscriptionData.builder();

        long durationSeconds = subscriptionDurationMinutes * 60L;
        long futureTimestamp = (System.currentTimeMillis() / 1000) + durationSeconds;

        if (subscriptionDurationMinutes < 2880) {
             subscriptionDataBuilder.setBillingCycleAnchor(futureTimestamp);
             logger.info("Using billing_cycle_anchor for short duration: {} minutes", subscriptionDurationMinutes);
        } else {
             subscriptionDataBuilder.setTrialEnd(futureTimestamp);
             logger.info("Using trial_end for duration: {} minutes", subscriptionDurationMinutes);
        }

        Map<String, String> subscriptionMetadata = new HashMap<>();
        subscriptionMetadata.put("company_id", companyId.toString());
        subscriptionMetadata.put("created_at", String.valueOf(System.currentTimeMillis()));
        subscriptionMetadata.put("source", "tideflow_checkout");

        subscriptionDataBuilder.putAllMetadata(subscriptionMetadata);

        Map<String, String> sessionMetadata = new HashMap<>();
        sessionMetadata.put("company_id", companyId.toString());
        sessionMetadata.put("customer_id", customerId);
        sessionMetadata.put("price_id", priceId);

        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(customerId)
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putAllMetadata(sessionMetadata)
                .setPaymentMethodCollection(SessionCreateParams.PaymentMethodCollection.ALWAYS)
                .setSubscriptionData(subscriptionDataBuilder.build())
                .build();

        logger.info("Created checkout session with metadata - company_id: {}, customer_id: {}, subscription_metadata_keys: {}",
                companyId, customerId, subscriptionMetadata.keySet());

        Session session = Session.create(params);
        logger.info("Created checkout session: {}", session.getId());

        return session.getUrl();
    }

    public Subscription createSubscription(String customerId, String priceId, int quantity) throws StripeException {
        logger.info("Creating subscription for customer: {}", customerId);

        com.stripe.param.SubscriptionCreateParams params = com.stripe.param.SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(com.stripe.param.SubscriptionCreateParams.Item.builder()
                        .setPrice(priceId)
                        .setQuantity((long) quantity)
                        .build())
                .setPaymentBehavior(com.stripe.param.SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .build();

        Subscription subscription = Subscription.create(params);
        logger.info("Created subscription: {}", subscription.getId());

        return subscription;
    }

    public Subscription updateSubscriptionQuantity(String subscriptionId, int quantity) throws StripeException {
        logger.info("Updating subscription quantity: {} to {}", subscriptionId, quantity);

        Subscription subscription = Subscription.retrieve(subscriptionId);

        if (subscription.getItems().getData().isEmpty()) {
            throw new IllegalArgumentException("Subscription has no items");
        }

        String subscriptionItemId = subscription.getItems().getData().get(0).getId();

        com.stripe.param.SubscriptionItemUpdateParams itemParams =
            com.stripe.param.SubscriptionItemUpdateParams.builder()
                .setQuantity((long) quantity)
                .build();

        com.stripe.model.SubscriptionItem subscriptionItem =
            com.stripe.model.SubscriptionItem.retrieve(subscriptionItemId);
        subscriptionItem.update(itemParams);

        Subscription updated = Subscription.retrieve(subscriptionId);
        logger.info("Updated subscription quantity");

        return updated;
    }

    public Subscription cancelSubscription(String subscriptionId) throws StripeException {
        logger.info("Canceling subscription: {}", subscriptionId);

        Subscription subscription = Subscription.retrieve(subscriptionId);
        Subscription canceled = subscription.cancel();
        logger.info("Canceled subscription");

        return canceled;
    }

    public Subscription getSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId);
    }

    public Customer getCustomer(String customerId) throws StripeException {
        return Customer.retrieve(customerId);
    }

    public String getPublishableKey() {
        return stripePublishableKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }
}
