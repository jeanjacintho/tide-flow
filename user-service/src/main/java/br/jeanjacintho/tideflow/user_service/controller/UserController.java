package br.jeanjacintho.tideflow.user_service.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.jeanjacintho.tideflow.user_service.dto.request.CreateUserRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.UpdateUserRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.UserResponseDTO;
import br.jeanjacintho.tideflow.user_service.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {
    
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody CreateUserRequestDTO requestDTO) {
        UserResponseDTO createdUser = userService.createUser(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(@RequestParam(required = false) String name, @RequestParam(required = false) String email, @RequestParam(required = false) String phone, @RequestParam(required = false) String city, @RequestParam(required = false) String state,@NonNull Pageable pageable) {
        return ResponseEntity.ok(userService.findAll(name, email, phone, city, state, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable @NonNull UUID id) {
        UserResponseDTO user = userService.findById(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable @NonNull UUID id, @Valid @RequestBody UpdateUserRequestDTO requestDTO) {
        UserResponseDTO updatedUser = userService.updateUser(id, requestDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable @NonNull UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}