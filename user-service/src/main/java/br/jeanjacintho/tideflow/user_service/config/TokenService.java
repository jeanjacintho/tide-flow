package br.jeanjacintho.tideflow.user_service.config;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import br.jeanjacintho.tideflow.user_service.model.User;

@Service
public class TokenService {
    @Value("${jwt.secret}")
    private String secret;

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String token =JWT.create().withIssuer("tideflow-user-service").withSubject(user.getEmail()).withExpiresAt(generateExpirationDate()).sign(algorithm);
            return token;
        } catch(JWTCreationException e) {
            throw new RuntimeException("Erro ao gerar token", e);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm).withIssuer("tideflow-user-service").build().verify(token).getSubject();
        } catch(JWTVerificationException e) {
            throw new RuntimeException("Token inválido", e);
        }
    }

    private Instant generateExpirationDate(){
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
