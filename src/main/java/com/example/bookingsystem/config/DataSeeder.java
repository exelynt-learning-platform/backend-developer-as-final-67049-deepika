package com.example.bookingsystem.config;

import com.example.bookingsystem.entity.Resource;
import com.example.bookingsystem.entity.Role;
import com.example.bookingsystem.entity.User;
import com.example.bookingsystem.repository.ResourceRepository;
import com.example.bookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

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
            userRepository.save(User.builder()
                    .username("admin")
                    .email("admin@bookingsystem.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build());
            log.info("Seeded ADMIN user -> username: admin / password: Admin@123");
        }

        if (!userRepository.existsByUsername("user")) {
            userRepository.save(User.builder()
                    .username("user")
                    .email("user@bookingsystem.com")
                    .password(passwordEncoder.encode("User@123"))
                    .role(Role.USER)
                    .enabled(true)
                    .build());
            log.info("Seeded USER user -> username: user / password: User@123");
        }

        if (!userRepository.existsByUsername("jane")) {
            userRepository.save(User.builder()
                    .username("jane")
                    .email("jane@bookingsystem.com")
                    .password(passwordEncoder.encode("Jane@123"))
                    .role(Role.USER)
                    .enabled(true)
                    .build());
            log.info("Seeded USER user -> username: jane / password: Jane@123");
        }
    }

    private void seedResources() {
        if (resourceRepository.count() == 0) {
            resourceRepository.save(Resource.builder()
                    .name("Conference Room A")
                    .description("Large meeting room with projector, seats 12")
                    .type("ROOM")
                    .available(true)
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Conference Room B")
                    .description("Small meeting room, seats 4")
                    .type("ROOM")
                    .available(true)
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Toyota Camry - Fleet Car 1")
                    .description("Company sedan for local trips")
                    .type("VEHICLE")
                    .available(true)
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Projector - Epson EX3280")
                    .description("Portable projector, HDMI/VGA")
                    .type("EQUIPMENT")
                    .available(true)
                    .build());

            log.info("Seeded {} sample resources", resourceRepository.count());
        }
    }
}
