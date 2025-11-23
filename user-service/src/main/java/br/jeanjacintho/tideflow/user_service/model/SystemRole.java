package br.jeanjacintho.tideflow.user_service.model;

/**
 * Roles do sistema (não vinculados a empresas).
 * SYSTEM_ADMIN tem acesso total a todas as empresas e dados.
 * NORMAL é o padrão para todos os usuários.
 */
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
