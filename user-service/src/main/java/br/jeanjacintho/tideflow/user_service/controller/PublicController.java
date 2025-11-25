package br.jeanjacintho.tideflow.user_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.jeanjacintho.tideflow.user_service.dto.request.RegisterCompanyRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.CompanyResponseDTO;
import br.jeanjacintho.tideflow.user_service.service.CompanyService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class PublicController {

    private final CompanyService companyService;

    @Autowired
    public PublicController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /**
     * Endpoint público para registro self-service de empresas.
     * Cria empresa, subscription FREE, departamento padrão, usuário OWNER e CompanyAdmin.
     */
    @PostMapping("/register-company")
    public ResponseEntity<CompanyResponseDTO> registerCompany(@Valid @RequestBody RegisterCompanyRequestDTO requestDTO) {
        CompanyResponseDTO company = companyService.registerCompanyPublic(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(company);
    }
}

