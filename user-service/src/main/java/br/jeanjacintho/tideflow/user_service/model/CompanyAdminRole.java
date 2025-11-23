package br.jeanjacintho.tideflow.user_service.model;

public enum CompanyAdminRole {
    OWNER("OWNER"),
    ADMIN("ADMIN"),
    HR_MANAGER("HR_MANAGER");

    private String role;

    CompanyAdminRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return this.role;
    }
}
