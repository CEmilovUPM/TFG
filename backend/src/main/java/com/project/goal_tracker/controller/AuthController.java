package com.project.goal_tracker.controller;

import com.project.goal_tracker.dto.LoginRequest;
import com.project.goal_tracker.dto.RefreshRequest;
import com.project.goal_tracker.dto.RegisterRequest;
import com.project.goal_tracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService service;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return service.verify(request);

    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        return service.refresh(request);
    }
}