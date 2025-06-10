package com.project.goal_tracker.misc;


import com.project.goal_tracker.model.User;
import com.project.goal_tracker.repository.UserRepository;
import com.project.goal_tracker.service.JWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class AdminUserInit implements CommandLineRunner {


    @Value("${app.admin.password}")
    private String adminPassword;

    @Autowired
    JWTService jwtService;

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public AdminUserInit(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        String adminEmail = "admin@goal.tracker";

        if (userRepository.findByEmail(adminEmail) == null) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setName("Admin");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setAdmin(true);
            admin.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
            admin.setRefreshToken(UUID.randomUUID().toString());

            userRepository.save(admin);
            System.out.println("Default admin user created.");
        } else {
            System.out.println("Admin user already exists. Skipping creation...");
        }
    }

}
