package br.jeanjacintho.tideflow.user_service.model;

public enum SubscriptionStatus {
    ACTIVE("ACTIVE"),
    TRIAL("TRIAL"),
    SUSPENDED("SUSPENDED"),
    CANCELLED("CANCELLED"),
    EXPIRED("EXPIRED");

    private String status;

    SubscriptionStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }
}
