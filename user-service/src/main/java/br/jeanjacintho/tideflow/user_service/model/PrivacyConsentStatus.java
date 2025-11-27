package br.jeanjacintho.tideflow.user_service.model;

/**
 * Status de consentimento de privacidade do usuário.
 * 
 * Garante que o usuário tenha controle explícito sobre o compartilhamento
 * de seus dados agregados com a empresa, mantendo sempre a privacidade
 * individual protegida.
 */
public enum PrivacyConsentStatus {
    /**
     * Usuário ainda não foi apresentado ao aviso de privacidade.
     * Dados não podem ser compartilhados até consentimento explícito.
     */
    PENDING("PENDING"),
    
    /**
     * Usuário leu e aceitou o aviso de privacidade.
     * Dados agregados podem ser compartilhados com a empresa (apenas por departamento,
     * nunca individualmente).
     */
    ACCEPTED("ACCEPTED"),
    
    /**
     * Usuário recusou o compartilhamento de dados agregados.
     * Apenas dados individuais para uso pessoal (app "My Tide") são processados.
     */
    DENIED("DENIED"),
    
    /**
     * Usuário revogou consentimento previamente dado.
     * Dados agregados não podem mais ser compartilhados.
     */
    REVOKED("REVOKED");

    private String status;

    PrivacyConsentStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }
    
    /**
     * Verifica se o status permite compartilhamento de dados agregados.
     * Apenas ACCEPTED permite compartilhamento.
     */
    public boolean allowsDataSharing() {
        return this == ACCEPTED;
    }
}
