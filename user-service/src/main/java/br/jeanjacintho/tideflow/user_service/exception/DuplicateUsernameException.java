package br.jeanjacintho.tideflow.user_service.exception;

public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String username) {
        super(String.format("Username já está em uso: %s", username));
    }
}
