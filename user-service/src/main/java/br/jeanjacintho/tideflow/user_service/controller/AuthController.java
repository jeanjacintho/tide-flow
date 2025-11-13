package br.jeanjacintho.tideflow.user_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.jeanjacintho.tideflow.user_service.config.TokenService;
import br.jeanjacintho.tideflow.user_service.dto.request.AuthenticationDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.RegisterDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.LoginResponseDTO;
import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.repository.UserRepository;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody AuthenticationDTO authenticationDTO) {
        var userPassword = new UsernamePasswordAuthenticationToken(authenticationDTO.email(), authenticationDTO.password());
        var auth = authenticationManager.authenticate(userPassword);
        
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado após autenticação"));
        
        String token = tokenService.generateToken(user);
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterDTO registerDTO) {
        if (userRepository.findByEmail(registerDTO.email()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        String encryptedPassword = passwordEncoder.encode(registerDTO.password());
        User user = new User(registerDTO.email(), encryptedPassword, registerDTO.role());
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }
}
