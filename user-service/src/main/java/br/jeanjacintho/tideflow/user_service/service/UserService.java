package br.jeanjacintho.tideflow.user_service.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import br.jeanjacintho.tideflow.user_service.dto.request.CreateUserRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.UpdateUserRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.UserResponseDTO;
import br.jeanjacintho.tideflow.user_service.exception.DuplicateEmailException;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.repository.UserRepository;
import br.jeanjacintho.tideflow.user_service.specifiction.UserSpecification;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponseDTO createUser(CreateUserRequestDTO requestDTO) {
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new DuplicateEmailException(requestDTO.getEmail());
        }

        User user = new User();
        user.setName(requestDTO.getName());
        user.setEmail(requestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        user.setPhone(requestDTO.getPhone());
        user.setAvatarUrl(requestDTO.getAvatarUrl());
        user.setCity(requestDTO.getCity());
        user.setState(requestDTO.getState());

        User savedUser = userRepository.save(user);
        return UserResponseDTO.fromEntity(savedUser);
    }

    public UserResponseDTO findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        return UserResponseDTO.fromEntity(user);
    }

    public Page<UserResponseDTO> findAll(@RequestParam(required = false) String name, @RequestParam(required = false) String email, @RequestParam(required = false) String phone, @RequestParam(required = false) String city, @RequestParam(required = false) String state, Pageable pageable) {
        Specification<User> specification = UserSpecification.withFilters(name, email, phone, city, state);
        return userRepository.findAll(specification, pageable).map(UserResponseDTO::fromEntity);
    }

    @Transactional
    public UserResponseDTO updateUser(UUID id, UpdateUserRequestDTO requestDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

        if (!existingUser.getEmail().equals(requestDTO.getEmail()) && 
            userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new DuplicateEmailException(requestDTO.getEmail());
        }

        existingUser.setName(requestDTO.getName());
        existingUser.setEmail(requestDTO.getEmail());
        if (requestDTO.getPassword() != null && !requestDTO.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        }
        existingUser.setPhone(requestDTO.getPhone());
        existingUser.setAvatarUrl(requestDTO.getAvatarUrl());
        existingUser.setCity(requestDTO.getCity());
        existingUser.setState(requestDTO.getState());

        User updatedUser = userRepository.save(existingUser);
        return UserResponseDTO.fromEntity(updatedUser);
    }

    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário", id);
        }
        userRepository.deleteById(id);
    }

    public Optional<UserResponseDTO> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserResponseDTO::fromEntity);
    }
}
