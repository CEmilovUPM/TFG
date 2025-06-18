package com.project.goal_tracker.dto;


import jakarta.validation.constraints.Positive;

public class GoalCreate {


    private String title;
    private String description;

    private String metric;

    private double totalDesiredAmount;


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

    public double getTotalDesiredAmount() {
        return totalDesiredAmount;
    }

    public void setTotalDesiredAmount(double totalDesiredAmount) {
        this.totalDesiredAmount = totalDesiredAmount;
    }
}
