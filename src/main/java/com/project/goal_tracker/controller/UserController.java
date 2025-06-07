package com.project.goal_tracker.controller;


import com.project.goal_tracker.model.CustomUserDetails;
import com.project.goal_tracker.service.JWTService;
import com.project.goal_tracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;



import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService service;

    @Autowired
    private JWTService jwtService;

    @RequestMapping("/profile")
    public ResponseEntity<?> profile(@AuthenticationPrincipal CustomUserDetails userDetails){
        return service.getProfile(userDetails.getUsername());
    }

}
