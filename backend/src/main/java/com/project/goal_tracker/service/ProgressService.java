package com.project.goal_tracker.service;

import com.project.goal_tracker.dto.ProgressCreate;
import com.project.goal_tracker.dto.ProgressResponse;
import com.project.goal_tracker.dto.ProgressUpdate;
import com.project.goal_tracker.model.Goal;
import com.project.goal_tracker.model.Progress;
import com.project.goal_tracker.model.User;
import com.project.goal_tracker.repository.GoalRepository;
import com.project.goal_tracker.repository.ProgressRepository;
import com.project.goal_tracker.utils.AggregateOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ProgressService {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private ProgressRepository progressRepository;

    private boolean validOwner(Long userId,  Goal goal, AggregateOutput<?> out){
        if(!Objects.equals(userId, goal.getUser().getId())){
            out.error(AggregateOutput.GOAL_NOT_FOUND,"Goal doesn't exist", HttpStatus.NOT_FOUND);
            return false;
        }
        return true;
    }

    private Progress lookupProgress(Goal goal, Long progressId, AggregateOutput<?> out){
        Optional<Progress> progOpt = progressRepository.findById(progressId);
        if(progOpt.isEmpty()){
            out.error(AggregateOutput.PROGRESS_NOT_FOUND,"Progress wasnt found",HttpStatus.NOT_FOUND);
            return null;
        }
        return progOpt.get();
    }

    private boolean progressRelatedToGoal(Progress prog, Goal goal, AggregateOutput<?> out){
        if(!Objects.equals(prog.getGoal().getId(), goal.getId())){
            out.error(AggregateOutput.PROGRESS_NOT_FOUND,"Progress wasnt found", HttpStatus.NOT_FOUND);
            return false;
        }
        return true;
    }

    public Goal retrieveGoal (Long userId, Long goalId, AggregateOutput<?> out){
        Optional<Goal> goalOpt = goalRepository.findById(goalId);
        if(goalOpt.isEmpty()){
            out.error(AggregateOutput.GOAL_NOT_FOUND,"Goal doesn't exist", HttpStatus.NOT_FOUND);
            return null;
        }
        Goal goal = goalOpt.get();
        if(!validOwner(userId, goal, out)){
            return null;
        }
        return goal;
    }



    public void listProgress(Goal goal, AggregateOutput<ProgressResponse> out){

        List<Progress> list = progressRepository.findByGoalId(goal.getId());

        for (Progress p:
             list) {
            out.append(ProgressResponse.fromEntity(p));
        }
        out.info("message", "Retrieved all progress related to the quota", HttpStatus.OK);
    }

    public void createProgress(Goal goal, ProgressCreate request, AggregateOutput<ProgressResponse> out){

        if(!validProgressObject(request.getAmount(),request.getUpdateNote(),out)){
            return;
        }
        Progress progress = new Progress();
        progress.setAmount(request.getAmount());
        progress.setUpdateNote(request.getUpdateNote());
        progress.setDate(LocalDate.now());
        progress.setGoal(goal);

        progressRepository.save(progress);
        out.append(ProgressResponse.fromEntity(progress));
        out.info("message", "Saved progress", HttpStatus.CREATED);
    }

    private boolean validProgressObject(Double amount, String updateNote, AggregateOutput<ProgressResponse> out){
        boolean isValid = true;
        if(amount <= 0.0){
            out.error("amount_must_be_positive", "The amount must be positive", HttpStatus.BAD_REQUEST);
            isValid = false;
        }
        if(updateNote.isBlank()){
            out.error("update_note_is_blank", "The update note cannot be blank", HttpStatus.BAD_REQUEST);
            isValid = false;
        }
        return isValid;
    }


    public void retrieveProgress(Goal goal, Long progressId, AggregateOutput<ProgressResponse> out) {
        Progress prog = lookupProgress(goal,progressId,out);
        if(prog == null){
            return;
        }
        if(!progressRelatedToGoal(prog, goal, out)){
            return;
        }
        out.append(ProgressResponse.fromEntity(prog));
        out.info("message","Found progress", HttpStatus.OK);
    }

    public void updateProgress(Goal goal, Long progressId, ProgressUpdate request, AggregateOutput<ProgressResponse> out){
        Progress prog = lookupProgress(goal,progressId,out);
        if(prog == null){
            return;
        }
        if(!progressRelatedToGoal(prog, goal, out)){
            return;
        }

        if(!validProgressObject(request.getAmount(),request.getUpdateNote(),out)){
            return;
        }

        if(request.getUpdateNote() != null) prog.setUpdateNote(request.getUpdateNote());
        if(request.getAmount() != null) prog.setAmount(request.getAmount());

        progressRepository.save(prog);
        out.append(ProgressResponse.fromEntity(prog));
        out.info("message","Successfully updated progress", HttpStatus.OK);

    }


    public void deleteProgress(Goal goal, Long progressId, AggregateOutput<ProgressResponse> out) {
        Progress prog = lookupProgress(goal,progressId,out);
        if(prog == null){
            return;
        }
        if(!progressRelatedToGoal(prog, goal, out)){
            return;
        }

        progressRepository.delete(prog);
        out.append(ProgressResponse.fromEntity(prog));
        out.info("message","Successfully deleted progress", HttpStatus.NO_CONTENT);
    }
}

