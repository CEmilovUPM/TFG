package com.project.goal_tracker.service;

import com.project.goal_tracker.dto.*;
import com.project.goal_tracker.model.User;
import com.project.goal_tracker.repository.UserRepository;
import com.project.goal_tracker.utils.AggregateOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.project.goal_tracker.dto.UserResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JWTService jwtService;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);




    public ResponseEntity<?> getProfile(String email,AggregateOutput<ProfileResponse> out){

        User user =  userRepository.findByEmail(email);
        if (user == null){
            out.error("user_not_found","User couldn't be found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(out.getOutput());
        }else{
            out.append(new ProfileResponse(user.getId(), user.getName(),user.getEmail(),user.isAdmin()));
            return ResponseEntity.status(HttpStatus.OK).body(out.getOutput());
        }

    }


    public ResponseEntity<?> verify(LoginRequest request) {

        AggregateOutput<String> out = new AggregateOutput<>();

        if (request.getEmail() == null || request.getEmail().isBlank() ||
                request.getPassword() == null || request.getPassword().isBlank()) {
            out.error("message", "Email and password must not be empty");
            return out.setStatus(HttpStatus.BAD_REQUEST).toResponseEntity();
        }

        try {
            Authentication authentication =
                    authManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(),
                            request.getPassword()));

            if(authentication.isAuthenticated()){
                User user = userRepository.findByEmail(request.getEmail());

                // If authentication is successful, generate the JWT token
                String accessToken = jwtService.generateToken(user.getEmail());
                String refreshToken = generateRefreshToken();

                user.setRefreshToken(refreshToken);
                user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
                userRepository.save(user);

                out.info("accessToken", accessToken);
                out.info("refreshToken", refreshToken);
                out.info("message", "Login successful");

                ResponseCookie accessTokenCookie = ResponseCookie.from("JWT", accessToken)
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .maxAge(Duration.ofHours(1))
                        .sameSite("Lax")
                        .build();

                ResponseCookie refreshTokenCookie = ResponseCookie.from("RefreshToken", refreshToken)
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .maxAge(Duration.ofDays(7))  // typically longer expiry for refresh token
                        .sameSite("Lax")
                        .build();

                return ResponseEntity
                        .ok()
                        .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                        .body(out.getOutput());
            }else{
                out.error("message", "Invalid credentials");
                return out.setStatus(HttpStatus.UNAUTHORIZED).toResponseEntity();
            }
        } catch (InternalAuthenticationServiceException e) {
            Throwable cause = e.getCause();
            if (cause instanceof DisabledException) {
                out.error("message", "User account is banned");
                return out.setStatus(HttpStatus.FORBIDDEN).toResponseEntity();
            }

            out.error("message", "Authentication failed");
            return out.setStatus(HttpStatus.UNAUTHORIZED).toResponseEntity();
        } catch (BadCredentialsException e) {
            // Catching invalid credentials specifically
            out.error("message", "Invalid credentials");
            return out.setStatus(HttpStatus.UNAUTHORIZED).toResponseEntity();
        }catch (Exception e) {
            out.error("message", "An error occurred during login");
            return out.setStatus(HttpStatus.INTERNAL_SERVER_ERROR).toResponseEntity();
        }
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public ResponseEntity<?> refresh(RefreshRequest request){

        AggregateOutput<String> out = new AggregateOutput<>();

        String refreshToken = request.getRefreshToken();

        if (refreshToken == null || refreshToken.isEmpty()) {
            out.error("message", "Refresh token is required");
            return out.setStatus(HttpStatus.BAD_REQUEST).toResponseEntity();
        }

        User user = userRepository.findByRefreshToken(refreshToken);

        if (user == null) {
            out.error("message", "Invalid refresh token");
            return out.setStatus(HttpStatus.UNAUTHORIZED).toResponseEntity();
        }

        if (request.getRefreshToken().equals(user.getRefreshToken())) {
            if(LocalDateTime.now().isBefore(user.getRefreshTokenExpiry())){
                String newAccessToken = jwtService.generateToken(user.getEmail());
                out.info("accessToken", newAccessToken);
                out.info("message","Successful token refresh");
                return out.setStatus(HttpStatus.OK).toResponseEntity();
            }else{
                out.error("message", "Please log in again");
                return out.setStatus(HttpStatus.UNAUTHORIZED).toResponseEntity();
            }
        } else {
            out.error("message", "Invalid refresh token");
            return out.setStatus(HttpStatus.UNAUTHORIZED).toResponseEntity();
        }
    }



    public ResponseEntity<?> register(RegisterRequest request) {

        AggregateOutput<String> out = new AggregateOutput<>();
        boolean validRegistration = true;

        if(invalidName(request.getName(), out)){
            validRegistration = false;
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            out.error("passwords_dont_match","Passwords do not match");
            validRegistration = false;
        }

        if(invalidPassword(request.getPassword(), out)){
            validRegistration = false;
        }

        if(invalidEmail(request.getEmail(), out)){
            validRegistration = false;
        }


        if (userRepository.findByEmail(request.getEmail()) != null) {
            out.info("email_already_registered","Email is already registered");
            validRegistration = false;
        }

        if(!validRegistration){
            return out.setStatus(HttpStatus.BAD_REQUEST).toResponseEntity();
        }


        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(encoder.encode(request.getPassword()));
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        user.setRefreshToken(generateRefreshToken());

        userRepository.save(user);

        String accessToken = jwtService.generateToken(user.getEmail());

        out.info("refreshToken",user.getRefreshToken());
        out.info("accessToken", accessToken);
        out.info("message","Registration successful");

        ResponseCookie accessTokenCookie = ResponseCookie.from("JWT", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofHours(1))
                .sameSite("Lax")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("RefreshToken", user.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))  //longer expiry for refresh token
                .sameSite("Lax")
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(out.getOutput());
    }

    private boolean invalidName(String name, AggregateOutput<?> out) {
        boolean hasError = false;

        if (name == null || name.isBlank()) {
            out.error("name_invalid_blank", "Name cannot be empty or blank");
            hasError = true;
        } else {
            if (!name.matches("^[a-zA-Z0-9\\s'-]+$")) {
                out.error("name_invalid_char", "Name contains invalid characters");
                hasError = true;
            }
            if (name.length() > 50) {
                out.error("name_invalid_length", "Name is too long. It should be less than 50 characters");
                hasError = true;
            }
        }

        return hasError;
    }

    private boolean invalidPassword(String password, AggregateOutput<?> out){
        boolean hasError = false;

        //For now this suffices
        if (password == null || password.isEmpty() || password.isBlank()) {
            out.error("password_invalid_blank", "Password cannot be empty or blank");
            return true;
        }
        if(!password.matches("^[a-zA-Z0-9]+$")){
            out.error("password_invalid_char", "Password contains non-alphanumeric values");
            hasError = true;
        }
        if (!password.matches(".*[a-z].*")) {
            out.error("password_invalid_no_lowercase", "Password must contain at least one lowercase letter");
            hasError = true;
        }
        if (!password.matches(".*[A-Z].*")) {
            out.error("password_invalid_no_uppercase", "Password must contain at least one uppercase letter");
            hasError = true;
        }
        if (!password.matches(".*\\d.*")) {
            out.error("password_invalid_no_number", "Password must contain at least one number");
            hasError = true;
        }
        if(password.length() < 8){
            out.error("password_invalid_small_length","Password must be at least 8 characters long");
            hasError = true;
        }
        if(password.length() > 50){
            out.error("password_invalid_long_length","Password is too long. It should be less than 50 characters");
            hasError = true;
        }
        return hasError;
    }

    private boolean invalidEmail(String email, AggregateOutput<?> out) {
        boolean hasError = false;
        if (email == null || email.isEmpty() || email.isBlank()) {
            out.error("email_invalid_blank", "Email cannot be empty or blank");
            return true;
        }
        // Regex provided by OWASP
        if (!email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            out.error("email_invalid_string", "Email is not valid");
            hasError = true;
        }
        if (email.length() > 150) {
            out.error("email_invalid_length", "Email is too long. It should be less than 150 characters");
            hasError = true;
        }
        return hasError;
    }


    public ResponseEntity<?> manageRole(String admin, Long userId, boolean makeAdmin) {
        AggregateOutput<String> out = new AggregateOutput<>();

        User targetUser = this.getUser(userId, out);
        if(targetUser == null){
            return out.toResponseEntity();
        }

        //default admin
        if(Objects.equals(targetUser.getEmail(), "admin@goal.tracker")){
            out.error("default_admin_protected", "This user cannot be modified.");
            return out.setStatus(HttpStatus.NOT_FOUND).toResponseEntity();
        }

        if(Objects.equals(admin, targetUser.getEmail())){
            out.warning("operation_not_allowed","You cannot demote/promote yourself");
            return out.setStatus(HttpStatus.BAD_REQUEST).toResponseEntity();
        }

        targetUser.setAdmin(makeAdmin);
        this.userRepository.save(targetUser);
         if(makeAdmin){
             out.info("promoted_admin", "User successfully promoted to admin");
         }else{
             out.info("demoted_admin", "User successfully demoted from admin");
         }
        return out.setStatus(HttpStatus.OK).toResponseEntity();
    }

    public ResponseEntity<?> manageAccountStatus(String admin, Long userId, boolean ban) {
        AggregateOutput<String> out = new AggregateOutput<>();

        User targetUser = this.getUser(userId, out);
        if(targetUser == null){
            return out.toResponseEntity();
        }

        //default admin
        if(Objects.equals(targetUser.getEmail(), "admin@goal.tracker")){
            out.error("default_admin_protected", "This user cannot be modified.");
            return out.setStatus(HttpStatus.NOT_FOUND).toResponseEntity();
        }

        if(Objects.equals(admin, targetUser.getEmail())){
            out.warning("operation_not_allowed","You cannot ban/unban yourself");
            return out.setStatus(HttpStatus.BAD_REQUEST).toResponseEntity();
        }
        System.out.println(targetUser);

        targetUser.setBanned(ban);
        this.userRepository.save(targetUser);
        if(ban){
            out.info("user_banned", "User successfully banned");
        }else{
            out.info("user_unbanned", "User successfully unbanned");
        }

        return out.setStatus(HttpStatus.OK).toResponseEntity();
    }

    public ResponseEntity<?> getUsers() {
        AggregateOutput<UserResponse> out = new AggregateOutput<>();
        List<User> users = userRepository.findAll();

        List<UserResponse> list = new ArrayList<>();
        for (User user: users) {
            list.add(UserResponse.fromEntity(user));
        }

        out.setData(list);
        out.info("message","Users succesfully retrieved",HttpStatus.OK);
        return out.toResponseEntity();
    }

    public User getUser(Long userId, AggregateOutput<?> out) {
        Optional<User> opt = userRepository.findById(userId);
        if (opt.isEmpty()) {
            out.error("user_not_found", "User not found", HttpStatus.NOT_FOUND);
            return null;
        }
        return opt.get();
    }

    public boolean validAction(User user, Long userId, AggregateOutput<?> out) {
        if (user == null){
            out.error("issuer_not_found","User issuing this action could not be found", HttpStatus.NOT_FOUND);
            return false;
        }
        Optional<User> targetUser = this.userRepository.findById(userId);
        if(targetUser.isEmpty()){
            out.error("target_user_not_found","Target user could not be found", HttpStatus.NOT_FOUND);
            return false;
        }
        if (user.isAdmin()){
            return true;
        }
        if(Objects.equals(user.getId(), userId)){
            return true;
        }
        out.error("illegal_action","You do not have permission to perform this action", HttpStatus.FORBIDDEN);
        return false;
    }

    public ResponseEntity<?> logout(User user) {
        AggregateOutput<String> out = new AggregateOutput<>();
        user.setRefreshTokenExpiry(LocalDateTime.now().minusHours(1));
        this.userRepository.save(user);
        out.info("logged_out","Successfully logged out");
        return out.setStatus(HttpStatus.OK).toResponseEntity();
    }
}