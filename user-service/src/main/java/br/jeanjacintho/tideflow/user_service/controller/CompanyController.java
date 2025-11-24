package br.jeanjacintho.tideflow.user_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.jeanjacintho.tideflow.user_service.dto.request.CompanyRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.InviteUserRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.CompanyResponseDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.DepartmentResponseDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.InviteUserResponseDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.UserResponseDTO;
import br.jeanjacintho.tideflow.user_service.service.CompanyService;
import br.jeanjacintho.tideflow.user_service.service.DepartmentService;
import br.jeanjacintho.tideflow.user_service.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/companies")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class CompanyController {

    private final CompanyService companyService;
    private final DepartmentService departmentService;
    private final UserService userService;

    @Autowired
    public CompanyController(CompanyService companyService, DepartmentService departmentService, UserService userService) {
        this.companyService = companyService;
        this.departmentService = departmentService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<CompanyResponseDTO> createCompany(@Valid @RequestBody CompanyRequestDTO requestDTO) {
        CompanyResponseDTO company = companyService.createCompany(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(company);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponseDTO> getCompanyById(@PathVariable @NonNull UUID id) {
        CompanyResponseDTO company = companyService.getCompanyById(id);
        return ResponseEntity.ok(company);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponseDTO> updateCompany(@PathVariable @NonNull UUID id, @Valid @RequestBody CompanyRequestDTO requestDTO) {
        CompanyResponseDTO company = companyService.updateCompany(id, requestDTO);
        return ResponseEntity.ok(company);
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponseDTO>> getAllCompanies() {
        List<CompanyResponseDTO> companies = companyService.getAllCompanies();
        return ResponseEntity.ok(companies);
    }

    @GetMapping("/{id}/departments")
    public ResponseEntity<List<DepartmentResponseDTO>> getCompanyDepartments(@PathVariable @NonNull UUID id) {
        List<DepartmentResponseDTO> departments = departmentService.getDepartmentsByCompany(id);
        return ResponseEntity.ok(departments);
    }

    @PostMapping("/{id}/departments")
    public ResponseEntity<DepartmentResponseDTO> createDepartment(@PathVariable @NonNull UUID id, @Valid @RequestBody br.jeanjacintho.tideflow.user_service.dto.request.DepartmentRequestDTO requestDTO) {
        DepartmentResponseDTO department = departmentService.createDepartment(id, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(department);
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<List<UserResponseDTO>> getCompanyUsers(@PathVariable @NonNull UUID id) {
        List<UserResponseDTO> users = userService.getUsersByCompany(id);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<InviteUserResponseDTO> inviteUser(@PathVariable @NonNull UUID id, @Valid @RequestBody InviteUserRequestDTO requestDTO) {
        InviteUserResponseDTO response = userService.inviteUser(id, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
