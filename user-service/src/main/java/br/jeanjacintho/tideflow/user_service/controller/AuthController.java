package br.jeanjacintho.tideflow.user_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
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
import br.jeanjacintho.tideflow.user_service.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserService userService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody AuthenticationDTO authenticationDTO) {
        try {
            var userPassword = new UsernamePasswordAuthenticationToken(
                    authenticationDTO.username() != null ? authenticationDTO.username().trim() : null,
                    authenticationDTO.password());

            var auth = authenticationManager.authenticate(userPassword);
            UserDetails userDetails = (UserDetails) auth.getPrincipal();

            User fullUser = userRepository.findByUsernameOrEmailWithCompanyAndDepartment(userDetails.getUsername())
                    .orElseThrow(() -> {
                        logger.error("Usuário não encontrado após autenticação: {}", userDetails.getUsername());
                        return new RuntimeException("Usuário não encontrado");
                    });

            String token = tokenService.generateToken(fullUser);
            return ResponseEntity.ok(new LoginResponseDTO(token));
        } catch (BadCredentialsException e) {
            logger.warn("Tentativa de login com credenciais inválidas para: {}", authenticationDTO.username());
            throw new BadCredentialsException("Credenciais inválidas");
        } catch (Exception e) {
            logger.error("Erro inesperado durante login: {}", e.getMessage(), e);
            throw new BadCredentialsException("Erro ao fazer login");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return ResponseEntity.ok().build();
    }
}
