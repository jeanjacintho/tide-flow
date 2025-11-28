package br.jeanjacintho.tideflow.user_service.exception;

import java.util.UUID;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String resource, UUID id) {
        super(String.format("Acesso negado ao recurso %s com ID: %s", resource, id));
    }

    public AccessDeniedException(String resource, UUID id, String reason) {
        super(String.format("Acesso negado ao recurso %s com ID: %s. Motivo: %s", resource, id, reason));
    }
}
