package com.project.goal_tracker.controller;

import com.project.goal_tracker.dto.ProgressCreate;
import com.project.goal_tracker.dto.ProgressResponse;
import com.project.goal_tracker.dto.ProgressUpdate;
import com.project.goal_tracker.model.CustomUserDetails;
import com.project.goal_tracker.model.Goal;
import com.project.goal_tracker.model.User;
import com.project.goal_tracker.service.ProgressService;
import com.project.goal_tracker.utils.AggregateOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/goals/{goalId}/progress")
public class ProgressController {

    @Autowired
    private ProgressService progService;



    @GetMapping("")
    public ResponseEntity<?> listProgress(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          @PathVariable Long goalId) {
        User user = userDetails.getUser();
        AggregateOutput<ProgressResponse> out = new AggregateOutput<>();
        Goal goal = progService.retrieveGoal(user,goalId,out);
        if(out.haveErrors()){
            return out.toResponseEntity();
        }

        progService.listProgress(goal,out);
        return out.toResponseEntity();
    }

    @GetMapping("/{progressId}")
    public ResponseEntity<?> retrieveProgress(@AuthenticationPrincipal CustomUserDetails userDetails,
                                              @PathVariable Long goalId,
                                              @PathVariable Long progressId){
        User user = userDetails.getUser();
        AggregateOutput<ProgressResponse> out = new AggregateOutput<>();
        Goal goal = progService.retrieveGoal(user,goalId,out);
        if(out.haveErrors()){
            return out.toResponseEntity();
        }

        progService.retrieveProgress(goal,progressId,out);
        return out.toResponseEntity();
    }

    @PostMapping("")
    public ResponseEntity<?> createProgress(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @PathVariable Long goalId,
                                            @RequestBody ProgressCreate request,
                                            BindingResult bindingResult) {
        User user = userDetails.getUser();
        AggregateOutput<ProgressResponse> out = new AggregateOutput<>();

        if(bindingResult.hasErrors()){
            bindingResult.getFieldErrors().forEach(error -> {
                out.error(error.getField(), error.getDefaultMessage());
            });
            return out.setStatus(HttpStatus.BAD_REQUEST).toResponseEntity();
        }

        Goal goal = progService.retrieveGoal(user,goalId,out);
        if(out.haveErrors()){
            return out.toResponseEntity();
        }

        progService.createProgress(goal,request,out);
        return out.toResponseEntity();
    }

    @PatchMapping("/{progressId}")
    ResponseEntity<?> updateProgress(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long goalId,
                                     @PathVariable Long progressId,
                                     @RequestBody ProgressUpdate request){
        User user = userDetails.getUser();
        AggregateOutput<ProgressResponse> out = new AggregateOutput<>();

        Goal goal = progService.retrieveGoal(user,goalId,out);
        if(out.haveErrors()){
            return out.toResponseEntity();
        }

        progService.updateProgress(goal, progressId, request, out);
        return out.toResponseEntity();
    }

    @DeleteMapping("/{progressId}")
    ResponseEntity<?> deleteProgress(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long goalId,
                                     @PathVariable Long progressId){
        User user = userDetails.getUser();
        AggregateOutput<ProgressResponse> out = new AggregateOutput<>();

        Goal goal = progService.retrieveGoal(user,goalId,out);

        if(out.haveErrors()){
            return out.toResponseEntity();
        }

        progService.deleteProgress(goal, progressId, out);
        return out.toResponseEntity();
    }







}
