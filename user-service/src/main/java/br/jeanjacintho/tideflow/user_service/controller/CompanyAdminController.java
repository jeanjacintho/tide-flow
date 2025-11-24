package br.jeanjacintho.tideflow.user_service.controller;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.jeanjacintho.tideflow.user_service.dto.request.AddCompanyAdminRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.CompanyAdminResponseDTO;
import br.jeanjacintho.tideflow.user_service.service.CompanyAdminService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/companies/{companyId}/admins")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class CompanyAdminController {

    private final CompanyAdminService companyAdminService;

    @Autowired
    public CompanyAdminController(CompanyAdminService companyAdminService) {
        this.companyAdminService = companyAdminService;
    }

    @PostMapping
    public ResponseEntity<CompanyAdminResponseDTO> addAdmin(
            @PathVariable @NonNull UUID companyId,
            @Valid @RequestBody AddCompanyAdminRequestDTO requestDTO) {
        UUID validatedCompanyId = Objects.requireNonNull(companyId, "companyId não pode ser nulo");
        UUID validatedUserId = Objects.requireNonNull(requestDTO.userId(), "userId não pode ser nulo");
        CompanyAdminResponseDTO admin = companyAdminService.addAdmin(validatedCompanyId, validatedUserId, requestDTO.role(), requestDTO.permissions());
        return ResponseEntity.status(HttpStatus.CREATED).body(admin);
    }

    @GetMapping
    public ResponseEntity<List<CompanyAdminResponseDTO>> getCompanyAdmins(@PathVariable @NonNull UUID companyId) {
        UUID validatedCompanyId = Objects.requireNonNull(companyId, "companyId não pode ser nulo");
        List<CompanyAdminResponseDTO> admins = companyAdminService.getCompanyAdmins(validatedCompanyId);
        return ResponseEntity.ok(admins);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeAdmin(@PathVariable @NonNull UUID companyId, @PathVariable @NonNull UUID userId) {
        UUID validatedCompanyId = Objects.requireNonNull(companyId, "companyId não pode ser nulo");
        UUID validatedUserId = Objects.requireNonNull(userId, "userId não pode ser nulo");
        companyAdminService.removeAdmin(validatedCompanyId, validatedUserId);
        return ResponseEntity.noContent().build();
    }
}
