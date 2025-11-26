package br.jeanjacintho.tideflow.user_service.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);
    
    private String stripeSecretKey;
    private String stripePublishableKey;
    private String webhookSecret;
    private String enterprisePriceId;
    public String frontendUrl;

    @PostConstruct
    public void init() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        
        stripeSecretKey = dotenv.get("STRIPE_SECRET_KEY");
        stripePublishableKey = dotenv.get("STRIPE_PUBLISHABLE_KEY");
        webhookSecret = dotenv.get("STRIPE_WEBHOOK_SECRET");
        enterprisePriceId = dotenv.get("STRIPE_ENTERPRISE_PRICE_ID");
        frontendUrl = dotenv.get("FRONTEND_URL", "http://localhost:3000");
        
        if (stripeSecretKey != null) {
            Stripe.apiKey = stripeSecretKey;
            logger.info("Stripe initialized with API key");
        } else {
            logger.warn("STRIPE_SECRET_KEY not found in environment variables");
        }
    }

    /**
     * Cria ou retorna um customer no Stripe
     */
    public String getOrCreateCustomer(UUID companyId, String companyName, String email) throws StripeException {
        logger.info("Getting or creating Stripe customer for company: {}", companyId);
        
        // Primeiro, tenta buscar customer existente pelo metadata
        Map<String, Object> params = new HashMap<>();
        params.put("limit", 100);
        
        // Se já existe customerId no banco, busca por ele
        // Caso contrário, cria um novo
        
        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(companyName)
                .putMetadata("company_id", companyId.toString())
                .build();
        
        Customer customer = Customer.create(customerParams);
        logger.info("Created Stripe customer: {}", customer.getId());
        
        return customer.getId();
    }

    /**
     * Cria um produto e preço no Stripe (se não existir)
     */
    public String getOrCreateEnterprisePrice() throws StripeException {
        if (enterprisePriceId != null && !enterprisePriceId.isEmpty()) {
            try {
                Price.retrieve(enterprisePriceId);
                return enterprisePriceId;
            } catch (StripeException e) {
                logger.warn("Configured price ID not found, creating new price");
            }
        }
        
        // Cria produto Enterprise
        ProductCreateParams productParams = ProductCreateParams.builder()
                .setName("TideFlow Enterprise Plan")
                .setDescription("Plano Enterprise - Usuários ilimitados e recursos avançados")
                .build();
        
        Product product = Product.create(productParams);
        logger.info("Created Stripe product: {}", product.getId());
        
        // Cria preço por usuário (€6.00 por usuário/mês)
        PriceCreateParams priceParams = PriceCreateParams.builder()
                .setProduct(product.getId())
                .setCurrency("eur")
                .setUnitAmount(600L) // €6.00 em centavos
                .setRecurring(PriceCreateParams.Recurring.builder()
                        .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                        .build())
                .putMetadata("plan_type", "ENTERPRISE")
                .build();
        
        Price price = Price.create(priceParams);
        logger.info("Created Stripe price: {}", price.getId());
        
        return price.getId();
    }

    /**
     * Cria uma sessão de checkout do Stripe
     */
    public String createCheckoutSession(UUID companyId, String customerId, String priceId, 
                                       String successUrl, String cancelUrl) throws StripeException {
        logger.info("Creating checkout session for company: {}", companyId);
        
        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(customerId)
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("company_id", companyId.toString())
                .setPaymentMethodCollection(SessionCreateParams.PaymentMethodCollection.ALWAYS)
                .build();
        
        Session session = Session.create(params);
        logger.info("Created checkout session: {}", session.getId());
        
        return session.getUrl();
    }

    /**
     * Cria uma subscription no Stripe
     */
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

    /**
     * Atualiza a quantidade de usuários em uma subscription
     */
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

    /**
     * Cancela uma subscription no Stripe
     */
    public Subscription cancelSubscription(String subscriptionId) throws StripeException {
        logger.info("Canceling subscription: {}", subscriptionId);
        
        Subscription subscription = Subscription.retrieve(subscriptionId);
        Subscription canceled = subscription.cancel();
        logger.info("Canceled subscription");
        
        return canceled;
    }

    /**
     * Retorna uma subscription do Stripe
     */
    public Subscription getSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId);
    }

    /**
     * Retorna um customer do Stripe
     */
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

