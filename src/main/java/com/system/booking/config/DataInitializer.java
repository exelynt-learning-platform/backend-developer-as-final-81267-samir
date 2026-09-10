package com.system.booking.config;

import com.system.booking.model.entity.Resource;
import com.system.booking.model.entity.User;
import com.system.booking.model.enums.Role;
import com.system.booking.repository.ResourceRepository;
import com.system.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Startup data seeder.
 *
 * SECURITY: This component is disabled in the 'prod' profile
 * (via @Profile("!prod")) to prevent seeding known-default credentials
 * into production environments.
 *
 * Enable seeding by NOT activating the 'prod' profile.
 * Override passwords via SEED_ADMIN_PASSWORD and SEED_USER_PASSWORD env vars.
 * Disable entirely by setting SEED_ENABLED=false (or spring.profiles.active=prod).
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password:admin123}")
    private String adminPassword;

    @Value("${app.seed.user-password:user123}")
    private String userPassword;

    @Override
    public void run(String... args) {
        seedUsers();
        seedResources();
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ROLE_ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Seeded default admin user: admin (password overridable via SEED_ADMIN_PASSWORD)");
        }

        if (!userRepository.existsByUsername("john_doe")) {
            User user = User.builder()
                    .username("john_doe")
                    .password(passwordEncoder.encode(userPassword))
                    .role(Role.ROLE_USER)
                    .build();
            userRepository.save(user);
            log.info("Seeded default user: john_doe (password overridable via SEED_USER_PASSWORD)");
        }
    }

    private void seedResources() {
        if (resourceRepository.count() == 0) {
            Resource conferenceRoom = Resource.builder()
                    .name("Executive Conference Room A")
                    .description("High-end conference room equipped with 4K video conferencing and whiteboard")
                    .type("CONFERENCE_ROOM")
                    .build();

            Resource projector = Resource.builder()
                    .name("Sony 4K Laser Projector")
                    .description("Portable 4K laser projector with wireless screen-mirroring capabilities")
                    .type("EQUIPMENT")
                    .build();

            resourceRepository.saveAll(List.of(conferenceRoom, projector));
            log.info("Seeded 2 default bookable resources");
        }
    }
}
