package com.project.goal_tracker.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class HomeController {

    // Serve the homepage
    @GetMapping("/")
    public String homePage(Model model) {
        return "index"; // This returns the 'index.html' Thymeleaf template
    }

    @GetMapping("/partials/login")
    public String loginForm() {
        return "partials/login"; // Fragment
    }

    @GetMapping("/partials/register")
    public String registerForm() {
        return "partials/register"; // Fragment
    }


}