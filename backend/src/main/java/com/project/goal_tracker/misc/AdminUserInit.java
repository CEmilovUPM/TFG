package com.project.goal_tracker.misc;


import com.project.goal_tracker.model.User;
import com.project.goal_tracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInit implements CommandLineRunner {


    @Value("${app.admin.password}")
    private String adminPassword;

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
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setAdmin(true);

            userRepository.save(admin);
            System.out.println("Default admin user created.");
        } else {
            System.out.println("Admin user already exists. Skipping creation...");
        }
    }

}
