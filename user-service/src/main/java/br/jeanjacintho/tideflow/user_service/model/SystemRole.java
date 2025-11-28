package br.jeanjacintho.tideflow.user_service.model;

public enum SystemRole {
    NORMAL("NORMAL"),
    SYSTEM_ADMIN("SYSTEM_ADMIN");

    private String role;

    SystemRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return this.role;
    }
}
