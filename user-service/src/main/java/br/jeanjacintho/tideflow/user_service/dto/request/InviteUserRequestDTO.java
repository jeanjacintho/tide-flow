package br.jeanjacintho.tideflow.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InviteUserRequestDTO(
    @NotBlank(message = "Nome é obrigatório")
    String name,

    String username,

    @Email(message = "Email deve ser válido")
    String email,

    @NotNull(message = "ID do departamento é obrigatório")
    UUID departmentId,

    String employeeId
) {
}
