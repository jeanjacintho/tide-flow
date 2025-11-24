package br.jeanjacintho.tideflow.user_service.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.jeanjacintho.tideflow.user_service.config.TenantContext;
import br.jeanjacintho.tideflow.user_service.dto.request.CompanyRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.CompanyResponseDTO;
import br.jeanjacintho.tideflow.user_service.exception.AccessDeniedException;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.CompanyStatus;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionPlan;
import br.jeanjacintho.tideflow.user_service.repository.CompanyRepository;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyAuthorizationService authorizationService;
    private final SubscriptionService subscriptionService;

    @Autowired
    public CompanyService(
            CompanyRepository companyRepository, 
            CompanyAuthorizationService authorizationService,
            SubscriptionService subscriptionService) {
        this.companyRepository = companyRepository;
        this.authorizationService = authorizationService;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public CompanyResponseDTO createCompany(CompanyRequestDTO requestDTO) {
        // Verifica se domain já existe
        if (requestDTO.domain() != null && !requestDTO.domain().isEmpty()) {
            if (companyRepository.existsByDomain(requestDTO.domain())) {
                throw new IllegalArgumentException("Domínio já está em uso: " + requestDTO.domain());
            }
        }

        Company company = new Company();
        company.setName(requestDTO.name());
        company.setDomain(requestDTO.domain());
        company.setBillingEmail(requestDTO.billingEmail());
        company.setBillingAddress(requestDTO.billingAddress());
        company.setTaxId(requestDTO.taxId());
        company.setSubscriptionPlan(SubscriptionPlan.FREE);
        company.setMaxEmployees(20);
        company.setStatus(CompanyStatus.TRIAL);

        Company savedCompany = companyRepository.save(company);
        
        // Cria assinatura automaticamente com plano FREE
        UUID companyId = savedCompany.getId();
        if (companyId != null) {
            final UUID finalCompanyId = companyId; // Final variable for null safety
            try {
                subscriptionService.createSubscription(finalCompanyId, SubscriptionPlan.FREE);
            } catch (Exception e) {
                // Log mas não falha a criação da empresa
                org.slf4j.LoggerFactory.getLogger(CompanyService.class)
                    .warn("Erro ao criar assinatura para empresa {}: {}", finalCompanyId, e.getMessage());
            }
        }
        
        return CompanyResponseDTO.fromEntity(savedCompany);
    }

    public CompanyResponseDTO getCompanyById(@NonNull UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", id));

        // Valida acesso (SYSTEM_ADMIN tem acesso total)
        if (!authorizationService.canAccessCompany(id)) {
            throw new AccessDeniedException("Empresa", id, "Usuário não tem acesso a esta empresa");
        }

        return CompanyResponseDTO.fromEntity(company);
    }

    @Transactional
    public CompanyResponseDTO updateCompany(@NonNull UUID id, CompanyRequestDTO requestDTO) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", id));

        // Valida acesso (SYSTEM_ADMIN ou OWNER/ADMIN da empresa)
        if (!TenantContext.isSystemAdmin() && !authorizationService.isOwner(id) && !authorizationService.isAdmin(id)) {
            throw new AccessDeniedException("Empresa", id, "Apenas OWNER ou ADMIN podem atualizar a empresa");
        }

        // Verifica se domain já existe (se mudou)
        if (requestDTO.domain() != null && !requestDTO.domain().isEmpty() 
            && !requestDTO.domain().equals(company.getDomain())) {
            if (companyRepository.existsByDomain(requestDTO.domain())) {
                throw new IllegalArgumentException("Domínio já está em uso: " + requestDTO.domain());
            }
        }

        company.setName(requestDTO.name());
        company.setDomain(requestDTO.domain());
        company.setBillingEmail(requestDTO.billingEmail());
        company.setBillingAddress(requestDTO.billingAddress());
        company.setTaxId(requestDTO.taxId());

        Company updatedCompany = companyRepository.save(company);
        return CompanyResponseDTO.fromEntity(updatedCompany);
    }

    public List<CompanyResponseDTO> getAllCompanies() {
        // SYSTEM_ADMIN vê todas as empresas, outros usuários só veem a própria
        if (TenantContext.isSystemAdmin()) {
            return companyRepository.findAll().stream()
                    .map(CompanyResponseDTO::fromEntity)
                    .collect(Collectors.toList());
        } else {
            UUID currentCompanyId = TenantContext.getCompanyId();
            if (currentCompanyId == null) {
                return List.of();
            }
            return companyRepository.findById(currentCompanyId)
                    .map(company -> List.of(CompanyResponseDTO.fromEntity(company)))
                    .orElse(List.of());
        }
    }
}
