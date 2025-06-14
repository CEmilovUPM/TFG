package com.project.goal_tracker.dto;

public class ProfileResponse {
    public String email;
    public String name;
    public boolean isAdmin;

    public Long id;

    public ProfileResponse(Long id, String name, String email, boolean isAdmin){
        this.id = id;
        this.name = name;
        this.email = email;
        this.isAdmin = isAdmin;
    }


}
