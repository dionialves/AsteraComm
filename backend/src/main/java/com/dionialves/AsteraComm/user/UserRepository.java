package com.dionialves.AsteraComm.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.core.userdetails.UserDetails;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.enabled = true ORDER BY u.name")
    List<User> findAllSummary();

    UserDetails findByUsername(String username);

    Optional<User> findUserByUsername(String username);

    boolean existsByUsername(String username);

    Page<User> findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(
            String name, String username, Pageable pageable);

    Page<User> findByRole(UserRole role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findByRoleAndSearch(@org.springframework.data.repository.query.Param("role") UserRole role,
                                   @org.springframework.data.repository.query.Param("search") String search,
                                   Pageable pageable);

    Page<User> findByEnabled(boolean enabled, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.enabled = :enabled AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findByEnabledAndSearch(@org.springframework.data.repository.query.Param("enabled") boolean enabled,
                                      @org.springframework.data.repository.query.Param("search") String search,
                                      Pageable pageable);
}
