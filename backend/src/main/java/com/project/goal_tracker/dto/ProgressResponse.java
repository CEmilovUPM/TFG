package com.project.goal_tracker.dto;

import com.project.goal_tracker.model.Progress;

import java.time.LocalDate;

public class ProgressResponse {

    private Long id;
    private String updateNote;

    private Double amount;

    private LocalDate date;

    public ProgressResponse(Long id, String updateNote, Double amount, LocalDate date) {
        this.id = id;
        this.updateNote = updateNote;
        this.amount = amount;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public static ProgressResponse fromEntity(Progress progress){
        return new ProgressResponse(
                progress.getId(),
                progress.getUpdateNote(),
                progress.getAmount(),
                progress.getDate()
        );
    }

}
