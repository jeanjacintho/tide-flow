package br.jeanjacintho.tideflow.user_service.model;

public enum BillingCycle {
    MONTHLY("MONTHLY"),
    YEARLY("YEARLY");

    private String cycle;

    BillingCycle(String cycle) {
        this.cycle = cycle;
    }

    public String getCycle() {
        return this.cycle;
    }
}
