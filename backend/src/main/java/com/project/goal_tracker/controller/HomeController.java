package com.project.goal_tracker.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class HomeController {

    // Serve the homepage
    @GetMapping({"/", "/index.html"})
    public String loginPage() {
        return "index";
    }

    @GetMapping("/partials/login")
    public String loginForm() {
        return "partials/login";
    }

    @GetMapping("/partials/register")
    public String registerForm() {
        return "partials/register";
    }

    @GetMapping("/home")
    public String mainPage() {
        return "main";
    }


}