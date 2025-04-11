package com.project.goal_tracker.service;

import com.project.goal_tracker.dto.LoginRequest;
import com.project.goal_tracker.dto.RegisterRequest;
import com.project.goal_tracker.model.User;
import com.project.goal_tracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JWTService jwtService;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public ResponseEntity<?> verify(LoginRequest user) {
        Map<String, String> response = new HashMap<>();

        try {
            Authentication authentication =
                    authManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));

            // If authentication is successful, generate the JWT token
            String jwtToken = jwtService.generateToken(user.getEmail());
            response.put("token", jwtToken);
            response.put("message", "Login successful.");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (BadCredentialsException e) {
            // Catching invalid credentials specifically
            response.put("message", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    public ResponseEntity<?> register(RegisterRequest request) {

        Map<String,String> response = new HashMap<>();

        if(validateName(request.getName()) != null){
            response.put("message",validateName(request.getName()));
            return ResponseEntity.badRequest().body(response);
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            response.put("message","Passwords do not match");
            return ResponseEntity.badRequest().body(response);
        }

        if(validatePassword(request.getPassword())!= null){
            response.put("message",validatePassword(request.getPassword()));
            return ResponseEntity.badRequest().body(response);
        }

        if(validateEmail(request.getEmail()) != null){
            response.put("message", validateEmail(request.getEmail()));
            return ResponseEntity.badRequest().body(response);
        }


        if (userRepository.findByEmail(request.getEmail()) != null) {
            response.put("message","Email is already registered");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }




        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(request.getPassword());

        user.setPassword(encoder.encode(user.getPassword()));
        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user.getEmail());
        response.put("token", jwtToken);
        response.put("message", "Registration successful.");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String validateName(String name){
        if (name == null || name.isEmpty() || name.isBlank()) {
            return "Name cannot be empty or blank";
        }
        if(!name.matches("^[a-zA-Z0-9\\s'-]+$")){
            return "Name contains non-alphanumeric values";
        }
        if(name.length() > 50){
            return "Name is too long. It should be less than 50 characters";
        }
        return null;
    }

    private String validatePassword(String password){

        //For now this suffices
        if (password == null || password.isEmpty() || password.isBlank()) {
            return "Password cannot be empty or blank";
        }
        if(!password.matches("^[a-zA-Z0-9]+$")){
            return "Password contains non-alphanumeric values";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one number";
        }
        if(password.length() < 8){
            return "Password must be at least 8 characters long";
        }
        if(password.length() > 50){
            return "Password is too long. It should be less than 50 characters";
        }
        return null;
    }

    private String validateEmail(String email) {

        if (email == null || email.isEmpty() || email.isBlank()) {
            return "Email cannot be empty or blank";
        }

        // Regex provided by OWASP
        if (!email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            return "Email is not valid";
        }

        if (email.length() > 150) {
            return "Email is too long. It should be less than 150 characters";
        }

        return null;
    }


}