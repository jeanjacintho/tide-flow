package br.jeanjacintho.tideflow.user_service.model;

public enum SubscriptionPlan {
    FREE("FREE"),
    ENTERPRISE("ENTERPRISE");

    private String plan;

    SubscriptionPlan(String plan) {
        this.plan = plan;
    }

    public String getPlan() {
        return this.plan;
    }
}
