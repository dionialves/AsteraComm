package com.dionialves.AsteraComm.auth;

import com.dionialves.AsteraComm.user.User;
import com.dionialves.AsteraComm.user.UserRepository;
import com.dionialves.AsteraComm.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    @Test
    void loadUserByUsername_shouldReturnUser_whenExists() {
        User user = new User("Test User", "user@test.com", "encoded", UserRole.ADMIN);
        when(userRepository.findByUsername("user@test.com")).thenReturn(user);

        UserDetails result = authorizationService.loadUserByUsername("user@test.com");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("user@test.com");
    }

    @Test
    void loadUserByUsername_shouldReturnNull_whenNotFound() {
        when(userRepository.findByUsername("nonexistent@test.com")).thenReturn(null);

        UserDetails result = authorizationService.loadUserByUsername("nonexistent@test.com");

        assertThat(result).isNull();
    }
}
