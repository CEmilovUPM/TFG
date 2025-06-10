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
@RequestMapping("/goals")
public class GoalController {


    @Autowired
    private GoalService goalService;

    @Autowired
    private UserService userService;

    @PostMapping("")
    public ResponseEntity<?> createGoal(@AuthenticationPrincipal CustomUserDetails userDetails,
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
            goalService.createGoals(user,request);
            out.info("message","Succesfully created the goal");
            return out.setStatus(HttpStatus.CREATED).toResponseEntity();
        }
    }
    @Transactional
    @GetMapping("")
    public ResponseEntity<?> listGoals (@AuthenticationPrincipal CustomUserDetails userDetails){
        User user = userDetails.getUser();
        AggregateOutput<GoalResponse> out = new AggregateOutput<>();

        if (user == null){
            out.error("user_not_found","User was not found",HttpStatus.BAD_GATEWAY);
            return out.toResponseEntity();
        }
        goalService.listGoals(user,out);
        return out.toResponseEntity();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> retrieveGoal (@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long id){
        User user = userDetails.getUser();

        AggregateOutput<GoalResponse> out = new AggregateOutput<>();
        goalService.retrieveGoal(user,id,out);

        return out.toResponseEntity();
    }

    @PatchMapping("/{id}")
    public  ResponseEntity<?> updateGoal(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         @PathVariable Long id,
                                         @RequestBody GoalUpdate request){
        User user = userDetails.getUser();

        AggregateOutput<GoalResponse> out = new AggregateOutput<>();
        goalService.updateGoal(user, id, request, out);

        return out.toResponseEntity();

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGoal(@AuthenticationPrincipal CustomUserDetails userDetails,
                                        @PathVariable Long id){
        User user = userDetails.getUser();
        AggregateOutput<String> out = new AggregateOutput<>();
        goalService.deleteGoal(user,id,out);
        return out.toResponseEntity();
    }


}
