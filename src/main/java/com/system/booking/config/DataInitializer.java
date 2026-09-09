package com.system.booking.config;

import com.system.booking.model.entity.Resource;
import com.system.booking.model.entity.User;
import com.system.booking.model.enums.Role;
import com.system.booking.repository.ResourceRepository;
import com.system.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedResources();
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ROLE_ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Seeded default admin user: admin / admin123");
        }

        if (!userRepository.existsByUsername("john_doe")) {
            User user = User.builder()
                    .username("john_doe")
                    .password(passwordEncoder.encode("user123"))
                    .role(Role.ROLE_USER)
                    .build();
            userRepository.save(user);
            log.info("Seeded default standard user: john_doe / user123");
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
            log.info("Seeded default bookable resources (Conference Room A, Sony 4K Laser Projector)");
        }
    }
}
