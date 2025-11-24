package br.jeanjacintho.tideflow.user_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.jeanjacintho.tideflow.user_service.dto.request.DepartmentRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.DepartmentResponseDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.UserResponseDTO;
import br.jeanjacintho.tideflow.user_service.service.DepartmentService;
import br.jeanjacintho.tideflow.user_service.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class DepartmentController {

    private final DepartmentService departmentService;
    private final UserService userService;

    @Autowired
    public DepartmentController(DepartmentService departmentService, UserService userService) {
        this.departmentService = departmentService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponseDTO> getDepartmentById(@PathVariable @NonNull UUID id) {
        DepartmentResponseDTO department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(department);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponseDTO> updateDepartment(@PathVariable @NonNull UUID id, @Valid @RequestBody DepartmentRequestDTO requestDTO) {
        DepartmentResponseDTO department = departmentService.updateDepartment(id, requestDTO);
        return ResponseEntity.ok(department);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable @NonNull UUID id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<List<UserResponseDTO>> getDepartmentUsers(@PathVariable @NonNull UUID id) {
        List<UserResponseDTO> users = userService.getUsersByDepartment(id);
        return ResponseEntity.ok(users);
    }
}
