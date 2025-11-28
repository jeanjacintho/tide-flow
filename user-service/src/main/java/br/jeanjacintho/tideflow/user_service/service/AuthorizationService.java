package br.jeanjacintho.tideflow.user_service.service;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.repository.UserRepository;

@Service("userDetailsService")
public class AuthorizationService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);
    private final UserRepository userRepository;

    public AuthorizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> {
                    logger.warn("Tentativa de login com usuário não encontrado: {}", username);
                    return new UsernameNotFoundException("Usuário não encontrado: " + username);
                });

        if (user.getIsActive() == null || !user.getIsActive()) {
            logger.warn("Tentativa de login com usuário inativo: {}", username);
            throw new UsernameNotFoundException("Usuário inativo: " + username);
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            logger.error("Usuário encontrado mas sem senha configurada: {}", username);
            throw new UsernameNotFoundException("Usuário sem senha configurada: " + username);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password(user.getPassword().trim())
                .authorities(getAuthorities(user))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(user.getMustChangePassword() != null && user.getMustChangePassword())
                .disabled(false)
                .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        if (user.getSystemRole() != null && user.getSystemRole() == br.jeanjacintho.tideflow.user_service.model.SystemRole.SYSTEM_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"));
        }

        return authorities;
    }
}
