package com.project.goal_tracker.controller;


import com.project.goal_tracker.dto.BanUserRequest;
import com.project.goal_tracker.dto.PromoteRequest;
import com.project.goal_tracker.model.CustomUserDetails;
import com.project.goal_tracker.service.JWTService;
import com.project.goal_tracker.service.UserService;
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

    @GetMapping("/profile")
    public ResponseEntity<?> profile(@AuthenticationPrincipal CustomUserDetails userDetails){
        return service.getProfile(userDetails.getUsername());
    }


    //Takes in the email of the targetUser
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/promote")
    public ResponseEntity<?> promote(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestBody PromoteRequest request){
        return service.manageRole(
                userDetails.getUsername(),
                request.getTargetUser(),
                true);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/demote")
    public ResponseEntity<?> demote(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestBody PromoteRequest request){
        return service.manageRole(
                userDetails.getUsername(),
                request.getTargetUser(),
                false);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/ban")
    public ResponseEntity<?> banUser(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestBody BanUserRequest request) {
        return service.manageAccountStatus(
                userDetails.getUsername(),
                request.getUser(),
                true);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/unban")
    public ResponseEntity<?> unbanUser(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       @RequestBody BanUserRequest request) {
        return service.manageAccountStatus(
                userDetails.getUsername(),
                request.getUser(),
                false);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/")
    public ResponseEntity<?> listUsers(@AuthenticationPrincipal CustomUserDetails userDetails){
        return service.getUsers();
    }
}
