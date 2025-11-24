package br.jeanjacintho.tideflow.user_service.exception;

public class InvalidPasswordException extends RuntimeException {
    
    public InvalidPasswordException(String message) {
        super(message);
    }
}
