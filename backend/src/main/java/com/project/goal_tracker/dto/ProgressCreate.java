package com.project.goal_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public class ProgressCreate {


    @NotBlank(message = "A description is required")
    private String updateNote;

    @NotNull(message =  "Amount is required")
    @Positive(message = "Amount must be positive")
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
