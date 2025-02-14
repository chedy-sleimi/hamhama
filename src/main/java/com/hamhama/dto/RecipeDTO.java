package com.hamhama.dto;

import com.hamhama.model.RecipeCategory;

public class RecipeDTO {
    private String name;
    private String description;
    private RecipeCategory category;

    // Constructors
    public RecipeDTO() {}

    public RecipeDTO(String name, String description, RecipeCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RecipeCategory getCategory() {
        return category;
    }

    public void setCategory(RecipeCategory category) {
        this.category = category;
    }
}
