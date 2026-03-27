package com.dionialves.AsteraComm.infra.security;

import com.dionialves.AsteraComm.user.User;
import com.dionialves.AsteraComm.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret-key-for-unit-tests");
    }

    private User buildUser(String username) {
        return new User("Test User", username, "encoded-password", UserRole.ADMIN);
    }

    @Test
    void generateToken_shouldReturnNonNullJwt() {
        User user = buildUser("user@test.com");
        String token = tokenService.generateToken(user);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_shouldEmbedUsernameAsSubject() {
        User user = buildUser("user@test.com");
        String token = tokenService.generateToken(user);
        String subject = tokenService.validateToken(token);
        assertThat(subject).isEqualTo("user@test.com");
    }

    @Test
    void validateToken_shouldReturnUsername_whenValid() {
        User user = buildUser("valid@test.com");
        String token = tokenService.generateToken(user);
        String result = tokenService.validateToken(token);
        assertThat(result).isEqualTo("valid@test.com");
    }
}
