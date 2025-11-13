package br.jeanjacintho.tideflow.user_service.service;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.repository.UserRepository;

@Service
public class AuthorizationService implements UserDetailsService {
    private final UserRepository userRepository;

    public AuthorizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com email: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(getAuthorities(user))
                .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return authorities;
    }
}
