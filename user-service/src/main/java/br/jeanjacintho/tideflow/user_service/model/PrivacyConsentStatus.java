package br.jeanjacintho.tideflow.user_service.model;

public enum PrivacyConsentStatus {

    PENDING("PENDING"),

    ACCEPTED("ACCEPTED"),

    DENIED("DENIED"),

    REVOKED("REVOKED");

    private String status;

    PrivacyConsentStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }

    public boolean allowsDataSharing() {
        return this == ACCEPTED;
    }
}
