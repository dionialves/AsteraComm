package com.dionialves.AsteraComm.user;

import com.dionialves.AsteraComm.exception.BusinessException;
import com.dionialves.AsteraComm.exception.NotFoundException;
import com.dionialves.AsteraComm.user.dto.UserCreateDTO;
import com.dionialves.AsteraComm.user.dto.UserResponseDTO;
import com.dionialves.AsteraComm.user.dto.UserUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("Test User", "user@test.com", "encoded-password", UserRole.ADMIN);
        testUser.setId(1L);
    }

    @Test
    void findAll_shouldReturnPagedResults() {
        Page<User> page = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<UserResponseDTO> result = userService.findAll(null, 0, 10, "name,asc");

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).username()).isEqualTo("user@test.com");
    }

    @Test
    void findAll_withSearch_shouldPassSearchToRepository() {
        Page<User> page = new PageImpl<>(List.of(testUser));
        when(userRepository.findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(
                eq("test"), eq("test"), any(Pageable.class))).thenReturn(page);

        Page<UserResponseDTO> result = userService.findAll("test", 0, 10, "name,asc");

        assertThat(result).hasSize(1);
        verify(userRepository).findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(
                eq("test"), eq("test"), any(Pageable.class));
    }

    @Test
    void findById_shouldReturnUserResponseDTO_whenExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponseDTO result = userService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("user@test.com");
    }

    @Test
    void findById_shouldThrowNotFoundException_whenNotExists() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_shouldEncodePasswordBeforeSaving() {
        UserCreateDTO dto = new UserCreateDTO("New User", "new@test.com", "rawpassword");
        when(userRepository.existsByUsername("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("rawpassword")).thenReturn("encoded-rawpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponseDTO result = userService.create(dto);

        verify(passwordEncoder).encode("rawpassword");
        verify(userRepository).save(argThat(u -> u.getPassword().equals("encoded-rawpassword")));
    }

    @Test
    void create_shouldThrowBusinessException_whenUsernameAlreadyExists() {
        UserCreateDTO dto = new UserCreateDTO("Test User", "user@test.com", "password123");
        when(userRepository.existsByUsername("user@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username already in use");
    }

    @Test
    void update_shouldUpdateFieldsAndSave() {
        UserUpdateDTO dto = new UserUpdateDTO("Updated Name", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponseDTO result = userService.update(1L, dto);

        assertThat(result).isNotNull();
        verify(userRepository).save(testUser);
    }

    @Test
    void update_shouldThrowNotFoundException_whenUserNotExists() {
        UserUpdateDTO dto = new UserUpdateDTO("Updated Name", true);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(99L, dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updatePassword_shouldEncodePasswordAndSave() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-new");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.updatePassword(1L, "newPassword123");

        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(argThat(u -> u.getPassword().equals("encoded-new")));
    }

    @Test
    void delete_shouldCallRepositoryDelete() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowNotFoundException_whenUserNotExists() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getCurrentUser_shouldReturnAuthenticatedUser() {
        var auth = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserResponseDTO result = userService.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("user@test.com");

        SecurityContextHolder.clearContext();
    }

    // --- findAllSummary ---

    @Test
    void findAllSummary_shouldDelegateToRepository() {
        when(userRepository.findAllSummary()).thenReturn(List.of(testUser));

        List<UserResponseDTO> result = userService.findAllSummary();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("user@test.com");
        verify(userRepository).findAllSummary();
    }

    @Test
    void findAllSummary_shouldReturnEmptyList_whenNoUsers() {
        when(userRepository.findAllSummary()).thenReturn(List.of());

        List<UserResponseDTO> result = userService.findAllSummary();

        assertThat(result).isEmpty();
    }
}
