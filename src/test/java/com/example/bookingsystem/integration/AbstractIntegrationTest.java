package com.example.bookingsystem.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.bookingsystem.entity.Resource;
import com.example.bookingsystem.entity.Role;
import com.example.bookingsystem.entity.User;
import com.example.bookingsystem.repository.ReservationRepository;
import com.example.bookingsystem.repository.ResourceRepository;
import com.example.bookingsystem.repository.UserRepository;
import com.example.bookingsystem.security.JwtUtil;
import org.springframework.security.core.userdetails.UserDetailsService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ResourceRepository resourceRepository;

    @Autowired
    protected ReservationRepository reservationRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtUtil jwtUtil;

    @Autowired
    protected UserDetailsService userDetailsService;

    protected User adminUser;
    protected User normalUser;
    protected User secondUser;
    protected Resource testResource;

    @BeforeEach
    void baseSetUp() {
        reservationRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = userRepository.save(User.builder()
                .username("admin")
                .email("admin@test.com")
                .password(passwordEncoder.encode("Admin@123"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        normalUser = userRepository.save(User.builder()
                .username("alice")
                .email("alice@test.com")
                .password(passwordEncoder.encode("Alice@123"))
                .role(Role.USER)
                .enabled(true)
                .build());

        secondUser = userRepository.save(User.builder()
                .username("bob")
                .email("bob@test.com")
                .password(passwordEncoder.encode("Bob@123"))
                .role(Role.USER)
                .enabled(true)
                .build());

        testResource = resourceRepository.save(Resource.builder()
                .name("Test Room")
                .description("A room for testing")
                .type("ROOM")
                .available(true)
                .build());
    }

    /** Generates a valid Bearer token string (including the "Bearer " prefix) for the given username. */
    protected String tokenFor(String username) {
        var userDetails = userDetailsService.loadUserByUsername(username);
        var user = userRepository.findByUsername(username).orElseThrow();
        return "Bearer " + jwtUtil.generateToken(userDetails, java.util.Map.of("role", user.getRole().name()));
    }
}
