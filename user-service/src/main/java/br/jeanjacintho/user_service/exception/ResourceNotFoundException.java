package br.jeanjacintho.user_service.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resource, Object id) {
        super(String.format("%s não encontrado com ID: %s", resource, id));
    }
}

