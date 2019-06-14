package com.melardev.spring.dtos.responses;


import com.melardev.spring.entities.Todo;

import java.time.LocalDateTime;

public class TodoSummaryDto {
    private final String title;
    private final boolean completed;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Long id;

    public TodoSummaryDto(Long id, String title, boolean completed, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getId() {
        return id;
    }

    public static TodoSummaryDto build(Todo todo) {
        return new TodoSummaryDto(todo.getId(), todo.getTitle(), todo.isCompleted(), todo.getCreatedAt(), todo.getUpdatedAt());
    }
}
