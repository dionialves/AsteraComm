package com.dionialves.AsteraComm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.dionialves.AsteraComm.user.User;
import com.dionialves.AsteraComm.user.UserRepository;
import com.dionialves.AsteraComm.user.UserRole;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${super.user.username}")
    private String superUsername;

    @Value("${super.user.password}")
    private String superPassword;

    @Value("${super.user.name}")
    private String superName;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        if (superPassword == null || superPassword.isEmpty()) {
            log.warn("⚠️  super.user.password not set! Skipping super user creation.");
            log.warn("⚠️  Set super.user.password in application properties to create super user.");
            return;
        }

        if (userRepository.existsByUsername(superUsername)) {
            log.info("✅ Super user '{}' already exists. Skipping creation.", superUsername);
            return;
        }

        User superUser = new User();
        superUser.setName(superName);
        superUser.setUsername(superUsername);
        superUser.setPassword(passwordEncoder.encode(superPassword));
        superUser.setRole(UserRole.ADMIN);
        superUser.setEnabled(true);

        userRepository.save(superUser);

        log.info("🎉 Super user '{}' created successfully!", superUsername);
        log.warn("⚠️  CHANGE THE SUPER USER PASSWORD IMMEDIATELY!");
    }
}
