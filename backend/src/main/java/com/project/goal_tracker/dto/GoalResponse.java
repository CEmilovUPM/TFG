package com.project.goal_tracker.dto;

import com.project.goal_tracker.model.Goal;

import java.time.LocalDate;

public class GoalResponse {


    private Long id;
    private String title;
    private String description;
    private String metric;
    private Double totalDesiredAmount;
    private LocalDate creationDate;

    private boolean isCompleted;


    public GoalResponse(Long id,
                        String title,
                        String description,
                        String metric,
                        Double totalDesiredAmount,
                        LocalDate creationDate,
                        boolean isCompleted) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.metric = metric;
        this.totalDesiredAmount = totalDesiredAmount;
        this.creationDate = creationDate;
        this.isCompleted = isCompleted;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public Double getTotalDesiredAmount() {
        return totalDesiredAmount;
    }

    public void setTotalDesiredAmount(Double totalDesiredAmount) {
        this.totalDesiredAmount = totalDesiredAmount;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }


    public static GoalResponse fromEntity(Goal goal) {
        return new GoalResponse(
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getMetric(),
                goal.getTotalDesiredAmount(),
                goal.getCreationDate(),
                goal.isCompleted()
        );
    }
}
