package br.jeanjacintho.tideflow.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequestDTO(
    @NotBlank(message = "Nome do departamento é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    String name,
    
    String description
) {
}
