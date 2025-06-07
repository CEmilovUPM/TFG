package com.project.goal_tracker.dto;

import java.time.LocalDate;

public class GoalUpdate {
    private String title;
    private String description;
    private String metric;
    private Double totalDesiredAmount;
    private LocalDate creationDate;

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
}
