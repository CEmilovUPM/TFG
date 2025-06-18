package com.project.goal_tracker.service;


import com.project.goal_tracker.dto.GoalCreate;
import com.project.goal_tracker.dto.GoalResponse;
import com.project.goal_tracker.dto.GoalUpdate;
import com.project.goal_tracker.model.Goal;
import com.project.goal_tracker.model.User;
import com.project.goal_tracker.repository.GoalRepository;
import com.project.goal_tracker.utils.AggregateOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class GoalService {

    @Autowired
    private GoalRepository goalRepository;


    public void createGoal(User user, GoalCreate request, AggregateOutput<?> out){
        if(!validGoalObject(request.getTitle(),request.getMetric(), request.getTotalDesiredAmount(), out)){
            return;
        }
        Goal goal = new Goal();
        goal.setUser(user);
        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setMetric(request.getMetric());
        goal.setTotalDesiredAmount(request.getTotalDesiredAmount());
        goal.setCreationDate(LocalDate.now());
        goalRepository.save(goal);
        out.info("message","Successfully created the goal",HttpStatus.CREATED);
    }

    public Goal lookupGoal(Long userId, Long id, AggregateOutput<?> out){
        Optional<Goal> opt = goalRepository.findById(id);
        if (opt.isEmpty()){
            out.error(AggregateOutput.GOAL_NOT_FOUND,"Goal doesn't exist", HttpStatus.NOT_FOUND);
            return null;
        }
        Goal goal = opt.get();
        if(!Objects.equals(userId, goal.getUser().getId())){
            out.error(AggregateOutput.GOAL_NOT_FOUND,"Goal doesn't exist", HttpStatus.NOT_FOUND);
            return null;
        }
        return goal;
    }


    public void listGoals(Long userId, AggregateOutput<GoalResponse> out){

        List<Goal> goals = goalRepository.findByUserId(userId);
        List<GoalResponse> list = new ArrayList<>();
        for (Goal goal : goals) {
            list.add(GoalResponse.fromEntity(goal));
        }
        out.setData(list);
        out.info("message","Goals related to the user successfully retrieved", HttpStatus.OK);
    }

    public void retrieveGoal(Long userId, Long id, AggregateOutput<GoalResponse> out){
        Goal goal = lookupGoal(userId, id, out);
        if(goal == null){
            return;
        }
        out.append(GoalResponse.fromEntity(goal));
        out.info("message","Successfully retrieved the goal",HttpStatus.OK);
    }

    @Transactional
    public void updateGoal(Long userId, Long id, GoalUpdate request, AggregateOutput<GoalResponse> out) {
        Goal goal = lookupGoal(userId, id, out);
        if(goal == null){
            return;
        }
        boolean isValid = true;
        if (request.getTotalDesiredAmount() != null) {
            if(request.getTotalDesiredAmount()<= 0.0){
                out.error("amount_must_be_positive", "The amount must be positive", HttpStatus.BAD_REQUEST);
                isValid = false;
            }else{
                goal.setTotalDesiredAmount(request.getTotalDesiredAmount());
            }
        }
        if (request.getTitle() != null){
            if(request.getTitle().isBlank()){
                out.error("title_is_blank", "The title cannot be blank", HttpStatus.BAD_REQUEST);
                isValid = false;
            }else{
                goal.setTitle(request.getTitle());
            }
        }
        if (request.getMetric() != null){
            if(request.getMetric().isBlank()){
                out.error("metric_is_blank", "The metric cannot be blank", HttpStatus.BAD_REQUEST);
                isValid = false;
            }else{
                goal.setMetric(request.getMetric());
            }
        }
        if (!isValid) return;


        if (request.getDescription() != null) goal.setDescription(request.getDescription());
        goal.setCompleted(request.isCompleted());
        goalRepository.save(goal);

        out.info("message","Successfully updated the goal",HttpStatus.OK);
    }

    @Transactional
    public void deleteGoal(Long userId, Long id, AggregateOutput<?> out){
        Goal goal = lookupGoal(userId, id, out);
        if(goal == null){
            return;
        }
        goalRepository.delete(goal);
        out.info("message","Successfully deleted the goal", HttpStatus.NO_CONTENT);
    }

    public boolean validGoalObject(String title, String metric, double totalDesiredAmount, AggregateOutput<?> out){
        boolean isValid = true;
        if(totalDesiredAmount <= 0.0){
            out.error("amount_must_be_positive", "The amount must be positive", HttpStatus.BAD_REQUEST);
            isValid = false;
        }
        if(title.isBlank()){
            out.error("title_is_blank", "The title cannot be blank", HttpStatus.BAD_REQUEST);
            isValid = false;
        }
        if(metric.isBlank()){
            out.error("metric_is_blank", "The metric cannot be blank", HttpStatus.BAD_REQUEST);
            isValid = false;
        }

        return isValid;

    }
}
