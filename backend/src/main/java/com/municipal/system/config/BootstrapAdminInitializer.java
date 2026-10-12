package com.municipal.system.config;

import com.municipal.system.user.entity.Role;
import com.municipal.system.user.entity.User;
import com.municipal.system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the initial system administrator on startup if configured in properties
 * and no administrator exists yet in the database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.BootstrapAdmin bootstrapAdmin = appProperties.bootstrapAdmin();
        if (bootstrapAdmin == null || !bootstrapAdmin.configured()) {
            log.info("No bootstrap administrator configured; skipping bootstrap creation.");
            return;
        }

        if (userRepository.countByRole(Role.ADMIN) > 0) {
            log.info("Administrator account already exists; skipping bootstrap administrator creation.");
            return;
        }

        if (userRepository.existsByEmail(bootstrapAdmin.email())) {
            log.warn("User with email '{}' already exists; skipping bootstrap admin.", bootstrapAdmin.email());
            return;
        }

        User adminUser = User.builder()
                .email(bootstrapAdmin.email())
                .passwordHash(passwordEncoder.encode(bootstrapAdmin.password()))
                .fullName(bootstrapAdmin.fullName())
                .role(Role.ADMIN)
                .build();

        userRepository.save(adminUser);
        log.info("Bootstrap administrator successfully initialized: {}", bootstrapAdmin.email());
    }
}
