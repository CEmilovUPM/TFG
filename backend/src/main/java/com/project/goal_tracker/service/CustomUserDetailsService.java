package com.project.goal_tracker.service;

import com.project.goal_tracker.model.CustomUserDetails;
import com.project.goal_tracker.model.User;
import com.project.goal_tracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;



@Service
public class CustomUserDetailsService implements UserDetailsService {

    private UserRepository userRepo;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepo){
        this.userRepo = userRepo;
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(email);
        if(user == null){
            throw new UsernameNotFoundException("User not found with email: " + email);

        }
        if (user.isBanned()) {
            throw new DisabledException("User is banned");
        }
        return new CustomUserDetails(user);
    }
}
