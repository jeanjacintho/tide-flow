package br.jeanjacintho.tideflow.user_service.model;

public enum CompanyStatus {
    ACTIVE("ACTIVE"),
    TRIAL("TRIAL"),
    SUSPENDED("SUSPENDED"),
    CANCELLED("CANCELLED");

    private String status;

    CompanyStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }
}
