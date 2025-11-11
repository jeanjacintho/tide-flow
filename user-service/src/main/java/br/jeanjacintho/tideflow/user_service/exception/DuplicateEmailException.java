package br.jeanjacintho.tideflow.user_service.exception;

public class DuplicateEmailException extends RuntimeException {
    
    public DuplicateEmailException(String email) {
        super(String.format("Email já está em uso: %s", email));
    }
}

