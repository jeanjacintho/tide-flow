package br.jeanjacintho.user_service.exception;

import java.util.List;

public class ValidationException extends RuntimeException {
    
    private final List<String> errors;
    
    public ValidationException(String message) {
        super(message);
        this.errors = List.of(message);
    }
    
    public ValidationException(List<String> errors) {
        super("Erros de validação encontrados");
        this.errors = errors;
    }
    
    public List<String> getErrors() {
        return errors;
    }
}

