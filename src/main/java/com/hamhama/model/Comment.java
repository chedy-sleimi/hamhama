package com.hamhama.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content; // The text content of the comment

    private LocalDateTime timestamp; // Timestamp of when the comment was posted

    @ManyToOne
    @JoinColumn(name = "user_id") // Link to the user who posted the comment
    private User user;

    @ManyToOne
    @JoinColumn(name = "recipe_id") // Link to the recipe that this comment belongs to
    private Recipe recipe;

    @PrePersist  // This method will run before the entity is persisted
    public void prePersist() {
        this.timestamp = LocalDateTime.now();  // Set current time
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @JsonIgnore  // This will exclude the 'user' object from serialization
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @JsonIgnore  // This will exclude the 'recipe' object from serialization
    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }
}
