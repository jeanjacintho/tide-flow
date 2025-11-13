package br.jeanjacintho.tideflow.user_service.model;

public enum UserRole {
    USER("USER"),
    ADMIN("ADMIN");

    private String role;

    UserRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return this.role;
    }
}
