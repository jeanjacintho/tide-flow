package br.jeanjacintho.user_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.jeanjacintho.user_service.dto.UserDTO;
import br.jeanjacintho.user_service.exception.DuplicateEmailException;
import br.jeanjacintho.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.user_service.model.User;
import br.jeanjacintho.user_service.repository.UserRepository;

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
    public UserDTO createUser(UserDTO userDTO) {
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new DuplicateEmailException(userDTO.getEmail());
        }

        User user = userDTO.toEntity();
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        User savedUser = userRepository.save(user);
        return UserDTO.fromEntity(savedUser);
    }

    public UserDTO findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        return UserDTO.fromEntity(user);
    }

    public List<UserDTO> findAll() {
        return userRepository.findAll().stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO updateUser(UUID id, UserDTO userDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

        if (!existingUser.getEmail().equals(userDTO.getEmail()) && 
            userRepository.existsByEmail(userDTO.getEmail())) {
            throw new DuplicateEmailException(userDTO.getEmail());
        }

        existingUser.setName(userDTO.getName());
        existingUser.setEmail(userDTO.getEmail());
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }
        existingUser.setDocument(userDTO.getDocument());
        existingUser.setPhone(userDTO.getPhone());
        existingUser.setAvatarUrl(userDTO.getAvatarUrl());

        User updatedUser = userRepository.save(existingUser);
        return UserDTO.fromEntity(updatedUser);
    }

    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário", id);
        }
        userRepository.deleteById(id);
    }

    public Optional<UserDTO> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserDTO::fromEntity);
    }
}
