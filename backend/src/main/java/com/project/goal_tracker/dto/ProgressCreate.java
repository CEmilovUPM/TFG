package com.project.goal_tracker.dto;

import java.time.LocalDate;

public class ProgressCreate {


    private String updateNote;

    private Double amount;

    public String getUpdateNote() {
        return updateNote;
    }

    public void setUpdateNote(String updateNote) {
        this.updateNote = updateNote;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
