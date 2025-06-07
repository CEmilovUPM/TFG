package com.project.goal_tracker.repository;

import com.project.goal_tracker.model.Goal;
import com.project.goal_tracker.model.Progress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgressRepository extends JpaRepository<Progress, Long> {

    List<Progress> findByGoalId(Long id);

}
