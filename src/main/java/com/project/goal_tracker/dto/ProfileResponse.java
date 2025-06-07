package com.project.goal_tracker.dto;

public class ProfileResponse {
    public String email;
    public String name;

    public ProfileResponse(String name, String email){
        this.name = name;
        this.email = email;
    }


}
