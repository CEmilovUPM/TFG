package com.project.goal_tracker.service;

import com.project.goal_tracker.dto.LoginRequest;
import com.project.goal_tracker.dto.ProfileResponse;
import com.project.goal_tracker.dto.RefreshRequest;
import com.project.goal_tracker.dto.RegisterRequest;
import com.project.goal_tracker.model.User;
import com.project.goal_tracker.repository.UserRepository;
import com.project.goal_tracker.utils.AggregateOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JWTService jwtService;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);




    public ResponseEntity<?> getProfile(String email){
        AggregateOutput<ProfileResponse> out = new AggregateOutput<>();
        User user =  userRepository.findByEmail(email);
        if (user == null){
            out.error("user_not_found","User couldn't be found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(out.getOutput());
        }else{
            out.append(new ProfileResponse(user.getName(),user.getEmail()));
            return ResponseEntity.status(HttpStatus.OK).body(out.getOutput());
        }

    }


    public ResponseEntity<?> verify(LoginRequest request) {
        Map<String, String> response = new HashMap<>();

        try {
            Authentication authentication =
                    authManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            if(authentication.isAuthenticated()){
                User user = userRepository.findByEmail(request.getEmail());

                // If authentication is successful, generate the JWT token
                String accessToken = jwtService.generateToken(user.getEmail());
                String refreshToken = generateRefreshToken();

                user.setRefreshToken(refreshToken);
                user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
                userRepository.save(user);

                response.put("accessToken", accessToken);
                response.put("refreshToken", refreshToken);
                response.put("message", "Login successful");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }else{
                response.put("message", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (BadCredentialsException e) {
            // Catching invalid credentials specifically
            response.put("message", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }catch (Exception e) {
            response.put("message", "An error occurred during login");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString(); // secure random string
    }

    public ResponseEntity<?> refresh(RefreshRequest request){
        Map<String, String> response = new HashMap<>();

        String refreshToken = request.getRefreshToken();

        if (refreshToken == null || refreshToken.isEmpty()) {
            response.put("message", "Refresh token is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        User user = userRepository.findByRefreshToken(refreshToken);

        if (user == null) {
            response.put("message", "Invalid refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (request.getRefreshToken().equals(user.getRefreshToken())) {
            if(LocalDateTime.now().isBefore(user.getRefreshTokenExpiry())){
                String newAccessToken = jwtService.generateToken(user.getEmail());
                response.put("accessToken", newAccessToken);
                return ResponseEntity.ok(response);
            }else{
                response.put("message", "Please log in again");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } else {
            response.put("message", "Invalid refresh token");
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
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        user.setRefreshToken(generateRefreshToken());
        userRepository.save(user);
        String accessToken = jwtService.generateToken(user.getEmail());

        response.put("refreshToken",user.getRefreshToken());
        response.put("accessToken", accessToken);
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