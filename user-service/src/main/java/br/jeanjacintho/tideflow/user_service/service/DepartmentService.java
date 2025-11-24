package br.jeanjacintho.tideflow.user_service.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.jeanjacintho.tideflow.user_service.config.TenantContext;
import br.jeanjacintho.tideflow.user_service.dto.request.DepartmentRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.DepartmentResponseDTO;
import br.jeanjacintho.tideflow.user_service.exception.AccessDeniedException;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.Department;
import br.jeanjacintho.tideflow.user_service.repository.CompanyRepository;
import br.jeanjacintho.tideflow.user_service.repository.DepartmentRepository;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CompanyRepository companyRepository;
    private final CompanyAuthorizationService authorizationService;

    @Autowired
    public DepartmentService(DepartmentRepository departmentRepository, 
                            CompanyRepository companyRepository,
                            CompanyAuthorizationService authorizationService) {
        this.departmentRepository = departmentRepository;
        this.companyRepository = companyRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public DepartmentResponseDTO createDepartment(@NonNull UUID companyId, DepartmentRequestDTO requestDTO) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));

        // Valida acesso
        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Empresa", companyId, "Usuário não tem acesso a esta empresa");
        }

        // Valida permissão para criar departamento
        if (!TenantContext.isSystemAdmin() && !authorizationService.canManageUsers(companyId)) {
            throw new AccessDeniedException("Departamento", null, "Apenas OWNER, ADMIN ou HR_MANAGER podem criar departamentos");
        }

        // Verifica se já existe departamento com mesmo nome na empresa
        if (departmentRepository.existsByCompanyIdAndName(companyId, requestDTO.name())) {
            throw new IllegalArgumentException("Já existe um departamento com o nome: " + requestDTO.name());
        }

        Department department = new Department(company, requestDTO.name(), requestDTO.description());
        Department savedDepartment = departmentRepository.save(department);
        return DepartmentResponseDTO.fromEntity(savedDepartment);
    }

    public DepartmentResponseDTO getDepartmentById(@NonNull UUID id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departamento", id));

        UUID companyId = department.getCompany().getId();
        
        // Valida acesso
        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Departamento", id, "Usuário não tem acesso a este departamento");
        }

        return DepartmentResponseDTO.fromEntity(department);
    }

    @Transactional
    public DepartmentResponseDTO updateDepartment(@NonNull UUID id, DepartmentRequestDTO requestDTO) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departamento", id));

        UUID companyId = department.getCompany().getId();
        
        // Valida acesso e permissão
        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Departamento", id, "Usuário não tem acesso a este departamento");
        }

        if (!TenantContext.isSystemAdmin() && !authorizationService.canManageUsers(companyId)) {
            throw new AccessDeniedException("Departamento", id, "Apenas OWNER, ADMIN ou HR_MANAGER podem atualizar departamentos");
        }

        // Verifica se nome já existe (se mudou)
        if (!requestDTO.name().equals(department.getName()) 
            && departmentRepository.existsByCompanyIdAndName(companyId, requestDTO.name())) {
            throw new IllegalArgumentException("Já existe um departamento com o nome: " + requestDTO.name());
        }

        department.setName(requestDTO.name());
        department.setDescription(requestDTO.description());

        Department updatedDepartment = departmentRepository.save(department);
        return DepartmentResponseDTO.fromEntity(updatedDepartment);
    }

    @Transactional
    public void deleteDepartment(@NonNull UUID id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departamento", id));

        UUID companyId = department.getCompany().getId();
        
        // Valida acesso e permissão
        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Departamento", id, "Usuário não tem acesso a este departamento");
        }

        if (!TenantContext.isSystemAdmin() && !authorizationService.isOwner(companyId) && !authorizationService.isAdmin(companyId)) {
            throw new AccessDeniedException("Departamento", id, "Apenas OWNER ou ADMIN podem deletar departamentos");
        }

        departmentRepository.delete(department);
    }

    public List<DepartmentResponseDTO> getDepartmentsByCompany(@NonNull UUID companyId) {
        // Valida acesso
        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Empresa", companyId, "Usuário não tem acesso a esta empresa");
        }

        return departmentRepository.findByCompanyId(companyId).stream()
                .map(DepartmentResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<DepartmentResponseDTO> getUsersByDepartment(@NonNull UUID departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Departamento", departmentId));

        UUID companyId = department.getCompany().getId();
        
        // Valida acesso
        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Departamento", departmentId, "Usuário não tem acesso a este departamento");
        }

        // Retorna lista vazia (usuários serão retornados por endpoint específico)
        return List.of();
    }
}
