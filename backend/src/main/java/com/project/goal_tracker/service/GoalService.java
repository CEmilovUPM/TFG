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


    public void createGoals(User user, GoalCreate request){
        Goal goal = new Goal();
        goal.setUser(user);
        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setMetric(request.getMetric());
        goal.setTotalDesiredAmount(request.getTotalDesiredAmount());
        goal.setCreationDate(LocalDate.now());
        goalRepository.save(goal);
    }


    private boolean validOwner(User user,  Goal goal, AggregateOutput<?> out){
        if(user.isAdmin()){
            return true;
        }
        if(!Objects.equals(user.getId(), goal.getUser().getId())){
            out.error(AggregateOutput.GOAL_NOT_FOUND,"Goal doesn't exist", HttpStatus.NOT_FOUND);
            return false;
        }
        return true;
    }

    public Goal lookupGoal(User user, Long id, AggregateOutput<?> out){
        Optional<Goal> goal = goalRepository.findById(id);
        if (goal.isEmpty()){
            out.error(AggregateOutput.GOAL_NOT_FOUND,"Goal doesn't exist", HttpStatus.NOT_FOUND);
            return null;
        }
        return goal.get();
    }

    public void listGoals(User user, AggregateOutput<GoalResponse> out){

        List<Goal> goals;
        if(user.isAdmin()){
            goals = goalRepository.findAll();
        }else{
            goals = goalRepository.findByUserId(user.getId());
        }

        List<GoalResponse> list = new ArrayList<>();
        for (Goal goal : goals) {
            list.add(GoalResponse.fromEntity(goal));
        }

        out.setData(list);
        out.info("message","Goals related to the user successfully retrieved",HttpStatus.OK);
    }

    public void retrieveGoal(User user, Long id, AggregateOutput<GoalResponse> out){
        Goal goal = lookupGoal(user, id, out);
        if(goal == null){
            return;
        }
        if(!validOwner(user, goal, out)){
            return;
        }
        out.append(GoalResponse.fromEntity(goal));
        out.info("message","Successfully retrieved the goal",HttpStatus.OK);

    }

    @Transactional
    public void updateGoal(User user, Long id, GoalUpdate request, AggregateOutput<GoalResponse> out) {
        Goal goal = lookupGoal(user, id, out);
        if(goal == null){
            return;
        }
        if(!validOwner(user, goal, out)){
            return;
        }

        if (request.getTitle() != null) goal.setTitle(request.getTitle());
        if (request.getMetric() != null) goal.setMetric(request.getMetric());
        if (request.getDescription() != null) goal.setDescription(request.getDescription());
        if (request.getTotalDesiredAmount() != null) goal.setTotalDesiredAmount(request.getTotalDesiredAmount());
        goalRepository.save(goal);

        out.info("message","Successfully updated the goal",HttpStatus.OK);
    }

    @Transactional
    public void deleteGoal(User user, Long id, AggregateOutput<?> out){
        Goal goal = lookupGoal(user, id, out);
        if(goal == null){
            return;
        }
        if(!validOwner(user, goal, out)){
            return;
        }

        goalRepository.delete(goal);
        out.info("message","Successfully deleted the goal", HttpStatus.NO_CONTENT);
    }


}
