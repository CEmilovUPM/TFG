package com.project.goal_tracker.dto;
import com.project.goal_tracker.model.User;


public final class UserResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final boolean banned;
    private final boolean admin;

    public UserResponse(Long id, String name, String email, boolean banned, boolean admin) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.banned = banned;
        this.admin = admin;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isBanned() {
        return banned;
    }

    public boolean isAdmin() {
        return admin;
    }

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isBanned(),
                user.isAdmin()
        );
    }
}