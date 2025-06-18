package com.project.goal_tracker.controller;


import com.project.goal_tracker.dto.GoalCreate;
import com.project.goal_tracker.dto.GoalResponse;
import com.project.goal_tracker.dto.GoalUpdate;
import com.project.goal_tracker.model.CustomUserDetails;
import com.project.goal_tracker.model.User;
import com.project.goal_tracker.service.GoalService;
import com.project.goal_tracker.service.UserService;
import com.project.goal_tracker.utils.AggregateOutput;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/{userId}/goals")
public class GoalController {


    @Autowired
    private GoalService goalService;

    @Autowired
    private UserService userService;

    @PostMapping("")
    public ResponseEntity<?> createGoal(@AuthenticationPrincipal CustomUserDetails userDetails,
                                        @PathVariable Long userId,
                                        @Valid @RequestBody GoalCreate request,
                                        BindingResult bindingResult){
        AggregateOutput<String> out = new AggregateOutput<>();
        if(bindingResult.hasErrors()){
            bindingResult.getFieldErrors().forEach(error -> {
                out.error(error.getField(), error.getDefaultMessage());
            });
            return out.setStatus(HttpStatus.BAD_REQUEST).toResponseEntity();
        }else{
            User user = userDetails.getUser();
            if (!userService.validAction(user, userId, out)){
                return out.setStatus(HttpStatus.BAD_REQUEST).toResponseEntity();
            }
            User targetUser = userService.getUser(userId, out);
            if (targetUser == null){
                return out.toResponseEntity();
            }
            goalService.createGoal(targetUser,request, out);
            return out.toResponseEntity();
        }
    }
    @Transactional
    @GetMapping("")
    public ResponseEntity<?> listGoals (@AuthenticationPrincipal CustomUserDetails userDetails,
                                        @PathVariable Long userId){
        User user = userDetails.getUser();
        AggregateOutput<GoalResponse> out = new AggregateOutput<>();

        if (!userService.validAction(user, userId, out)){
            return out.setStatus(HttpStatus.BAD_REQUEST).toResponseEntity();
        }

        goalService.listGoals(userId,out);
        return out.toResponseEntity();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> retrieveGoal (@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @PathVariable Long userId,
                                           @PathVariable Long id){
        User user = userDetails.getUser();
        AggregateOutput<GoalResponse> out = new AggregateOutput<>();

        if (!userService.validAction(user, userId, out)){
            return out.toResponseEntity();
        }

        goalService.retrieveGoal(userId,id,out);

        return out.toResponseEntity();
    }

    @PatchMapping("/{id}")
    public  ResponseEntity<?> updateGoal(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         @PathVariable Long userId,
                                         @PathVariable Long id,
                                         @RequestBody GoalUpdate request){
        User user = userDetails.getUser();
        AggregateOutput<GoalResponse> out = new AggregateOutput<>();
        if (!userService.validAction(user, userId, out)){
            return out.toResponseEntity();
        }
        goalService.updateGoal(userId, id, request, out);

        return out.toResponseEntity();

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGoal(@AuthenticationPrincipal CustomUserDetails userDetails,
                                        @PathVariable Long userId,
                                        @PathVariable Long id){
        User user = userDetails.getUser();
        AggregateOutput<String> out = new AggregateOutput<>();
        if (!userService.validAction(user, userId, out)){
            return out.toResponseEntity();
        }
        goalService.deleteGoal(userId,id,out);
        return out.toResponseEntity();
    }


}
