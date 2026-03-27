package com.dionialves.AsteraComm.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dionialves.AsteraComm.user.dto.UserCreateDTO;
import com.dionialves.AsteraComm.user.dto.UserResponseDTO;
import com.dionialves.AsteraComm.user.dto.UserUpdateDTO;
import com.dionialves.AsteraComm.exception.BusinessException;
import com.dionialves.AsteraComm.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAll(String search, int page, int size, String sort) {
        return findAll(search, null, page, size, sort);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAll(String search, Boolean enabled, int page, int size, String sort) {

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction sortDirection = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

        boolean hasSearch = search != null && !search.isBlank();
        Page<User> users;
        if (enabled != null && hasSearch) {
            users = userRepository.findByEnabledAndSearch(enabled, search, pageable);
        } else if (enabled != null) {
            users = userRepository.findByEnabled(enabled, pageable);
        } else if (hasSearch) {
            users = userRepository.findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(
                    search, search, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(UserResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAll(String search, int page, int size, String sort, UserRole role) {

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction sortDirection = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

        boolean hasSearch = search != null && !search.isBlank();
        Page<User> users;
        if (role != null && hasSearch) {
            users = userRepository.findByRoleAndSearch(role, search, pageable);
        } else if (role != null) {
            users = userRepository.findByRole(role, pageable);
        } else if (hasSearch) {
            users = userRepository.findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(
                    search, search, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(UserResponseDTO::new);
    }

    public UserResponseDTO findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + id));
        return new UserResponseDTO(user);
    }

    public UserResponseDTO create(UserCreateDTO dto) {
        if (userRepository.existsByUsername(dto.username())) {
            throw new BusinessException("Username already in use");
        }

        User user = new User(
                dto.name(),
                dto.username(),
                passwordEncoder.encode(dto.password()),
                UserRole.ADMIN);

        User saved = userRepository.save(user);
        return new UserResponseDTO(saved);
    }

    public UserResponseDTO update(Long id, UserUpdateDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + id));

        if (dto.name() != null)
            user.setName(dto.name());
        if (dto.enabled() != null)
            user.setEnabled(dto.enabled());

        User saved = userRepository.save(user);
        return new UserResponseDTO(saved);
    }

    public UserResponseDTO updatePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + id));

        user.setPassword(passwordEncoder.encode(newPassword));
        User saved = userRepository.save(user);
        return new UserResponseDTO(saved);
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }

    public List<UserResponseDTO> findAllSummary() {
        return userRepository.findAllSummary().stream()
                .map(UserResponseDTO::new)
                .toList();
    }

    public UserResponseDTO getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        return new UserResponseDTO(user);
    }
}
