package br.jeanjacintho.tideflow.user_service.dto.request;

import br.jeanjacintho.tideflow.user_service.model.UserRole;

public record RegisterDTO(String email, String password, UserRole role) {
    
}
