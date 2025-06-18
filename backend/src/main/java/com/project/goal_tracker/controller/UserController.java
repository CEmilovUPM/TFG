package com.project.goal_tracker.controller;


import com.project.goal_tracker.dto.BanUserRequest;
import com.project.goal_tracker.dto.ProfileResponse;
import com.project.goal_tracker.dto.PromoteRequest;
import com.project.goal_tracker.model.CustomUserDetails;
import com.project.goal_tracker.model.User;
import com.project.goal_tracker.service.JWTService;
import com.project.goal_tracker.service.UserService;
import com.project.goal_tracker.utils.AggregateOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService service;

    @Autowired
    private JWTService jwtService;


    //this one will be used to get your JWT profile
    @GetMapping("/profile")
    public ResponseEntity<?> profile(@AuthenticationPrincipal CustomUserDetails userDetails){
        AggregateOutput<ProfileResponse> out = new AggregateOutput<>();
        return service.getProfile(userDetails.getUsername(),out);
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<?> profileFromUser(@AuthenticationPrincipal CustomUserDetails userDetails,
                                             @PathVariable Long userId){
        AggregateOutput<ProfileResponse> out = new AggregateOutput<>();
        User user = service.getUser(userId, out);
        return service.getProfile(user.getEmail(),out);
    }

    //Takes in the email of the targetUser
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userId}/promote")
    public ResponseEntity<?> promote(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long userId){
        return service.manageRole(
                userDetails.getUsername(),
                userId,
                true);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userId}/demote")
    public ResponseEntity<?> demote(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @PathVariable Long userId){
        return service.manageRole(
                userDetails.getUsername(),
                userId,
                false);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userId}/ban")
    public ResponseEntity<?> banUser(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long userId) {
        return service.manageAccountStatus(
                userDetails.getUsername(),
                userId,
                true);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userId}/unban")
    public ResponseEntity<?> unbanUser(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       @PathVariable Long userId) {
        return service.manageAccountStatus(
                userDetails.getUsername(),
                userId,
                false);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("")
    public ResponseEntity<?> listUsers(@AuthenticationPrincipal CustomUserDetails userDetails){
        return service.getUsers();
    }
}
