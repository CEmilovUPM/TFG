package com.project.goal_tracker.repository;

import com.project.goal_tracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByName(String username);
    User findByEmail(String email);
}