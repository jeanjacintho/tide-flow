package br.jeanjacintho.tideflow.user_service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import br.jeanjacintho.tideflow.user_service.model.Department;

public record DepartmentResponseDTO(
    UUID id,
    UUID companyId,
    String name,
    String description,
    LocalDateTime createdAt
) {
    public static DepartmentResponseDTO fromEntity(Department department) {
        return new DepartmentResponseDTO(
            department.getId(),
            department.getCompany().getId(),
            department.getName(),
            department.getDescription(),
            department.getCreatedAt()
        );
    }
}
